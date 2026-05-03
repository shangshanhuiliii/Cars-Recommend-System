param(
    [string]$ConfigPath = "backend/src/main/resources/application-local.yml",
    [string]$HostName = "",
    [int]$Port = 0,
    [string]$DatabaseName = "",
    [string]$User = "",
    [switch]$Recreate,
    [switch]$Force,
    [switch]$RecalculateScores,
    [string]$BackendUrl = "http://localhost:8080",
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host @"
Initialize the local MySQL database for Cars-Recommend-System.

Examples:
  powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1
  powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -Recreate
  powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -HostName localhost -Port 3306 -DatabaseName cars_recommend_system -User root -Recreate
  powershell -ExecutionPolicy Bypass -File .\scripts\init-dev-db.ps1 -RecalculateScores

What it does:
  1. Creates the database if needed.
  2. Runs backend/src/main/resources/db/schema.sql.
  3. Runs backend/src/main/resources/db/seed-data.sql.
  4. Prints imported row counts.

Use -Recreate only for a local development database. It drops the target database.
"@
}

if ($Help) {
    Show-Help
    exit 0
}

function ConvertFrom-PlainValue([string]$Value) {
    if ($null -eq $Value) {
        return ""
    }
    $trimmed = $Value.Trim()
    if (($trimmed.StartsWith('"') -and $trimmed.EndsWith('"')) -or ($trimmed.StartsWith("'") -and $trimmed.EndsWith("'"))) {
        return $trimmed.Substring(1, $trimmed.Length - 2)
    }
    return $trimmed
}

function Read-LocalConfig([string]$Path) {
    $result = @{
        HostName = ""
        Port = 0
        DatabaseName = ""
        User = ""
        Password = ""
    }

    if (-not (Test-Path -LiteralPath $Path)) {
        return $result
    }

    $content = Get-Content -LiteralPath $Path -Encoding UTF8
    foreach ($line in $content) {
        if ($line -match '^\s*url:\s*(.+?)\s*$') {
            $url = ConvertFrom-PlainValue $Matches[1]
            if ($url -match 'jdbc:mysql://([^:/?]+)(?::([0-9]+))?/([^?]+)') {
                $result.HostName = $Matches[1]
                if ($Matches[2]) {
                    $result.Port = [int]$Matches[2]
                }
                $result.DatabaseName = $Matches[3]
            }
        }
        elseif ($line -match '^\s*username:\s*(.+?)\s*$') {
            $result.User = ConvertFrom-PlainValue $Matches[1]
        }
        elseif ($line -match '^\s*password:\s*(.*?)\s*$') {
            $result.Password = ConvertFrom-PlainValue $Matches[1]
        }
    }

    if ($result.Password -eq "your_mysql_password") {
        $result.Password = ""
    }

    return $result
}

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Assert-DatabaseName([string]$Name) {
    if ([string]::IsNullOrWhiteSpace($Name) -or $Name -notmatch '^[A-Za-z0-9_]+$') {
        throw "DatabaseName must contain only letters, numbers, and underscores."
    }
}

function Invoke-MySql {
    param(
        [string[]]$Arguments
    )

    & $script:MysqlCommand @script:MysqlCommonArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "mysql command failed with exit code $LASTEXITCODE."
    }
}

function Invoke-MySqlQuery {
    param(
        [string]$DatabaseName,
        [string]$Query
    )

    $args = @("--batch", "--skip-column-names", "--database=$DatabaseName", "--execute=$Query")
    $output = & $script:MysqlCommand @script:MysqlCommonArgs @args
    if ($LASTEXITCODE -ne 0) {
        throw "mysql query failed with exit code $LASTEXITCODE."
    }
    return $output
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$schemaPath = Join-Path $repoRoot "backend/src/main/resources/db/schema.sql"
$seedPath = Join-Path $repoRoot "backend/src/main/resources/db/seed-data.sql"

if (-not (Test-Path -LiteralPath $schemaPath)) {
    throw "schema.sql not found: $schemaPath"
}
if (-not (Test-Path -LiteralPath $seedPath)) {
    throw "seed-data.sql not found: $seedPath"
}

$configFullPath = Join-Path $repoRoot $ConfigPath
$config = Read-LocalConfig $configFullPath

if ([string]::IsNullOrWhiteSpace($HostName)) {
    $HostName = if ($config.HostName) { $config.HostName } else { "localhost" }
}
if ($Port -le 0) {
    $Port = if ($config.Port -gt 0) { $config.Port } else { 3306 }
}
if ([string]::IsNullOrWhiteSpace($DatabaseName)) {
    $DatabaseName = if ($config.DatabaseName) { $config.DatabaseName } else { "cars_recommend_system" }
}
if ([string]::IsNullOrWhiteSpace($User)) {
    $User = if ($config.User) { $config.User } else { "root" }
}

Assert-DatabaseName $DatabaseName

$password = $config.Password
if ([string]::IsNullOrEmpty($password)) {
    $secure = Read-Host -Prompt "MySQL password for user '$User'" -AsSecureString
    $password = ConvertTo-PlainText $secure
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "mysql command not found. Install MySQL 8 client and make sure mysql is in PATH."
}

$script:MysqlCommand = $mysql.Source
$defaultsFile = [System.IO.Path]::GetTempFileName()

try {
    $escapedPassword = $password.Replace('\', '\\').Replace('"', '\"')
    $defaultsContent = @(
        "[client]"
        "host=$HostName"
        "port=$Port"
        "user=$User"
        "password=""$escapedPassword"""
        "default-character-set=utf8mb4"
    )
    Set-Content -LiteralPath $defaultsFile -Value $defaultsContent -Encoding ASCII

    $script:MysqlCommonArgs = @(
        "--defaults-extra-file=$defaultsFile",
        "--protocol=tcp",
        "--default-character-set=utf8mb4"
    )

    Write-Host "Target database: $DatabaseName on ${HostName}:$Port"

    if ($Recreate) {
        if (-not $Force) {
            Write-Host "This will DROP and recreate database '$DatabaseName'."
            $confirm = Read-Host "Type the database name to continue"
            if ($confirm -ne $DatabaseName) {
                Write-Host "Cancelled."
                exit 1
            }
        }
        Invoke-MySql @("--execute=DROP DATABASE IF EXISTS ``$DatabaseName``;")
    }
    else {
        Write-Host "Recreate mode is off. If this database already contains seed rows, importing seed-data.sql may fail on duplicate IDs."
    }

    Invoke-MySql @("--execute=CREATE DATABASE IF NOT EXISTS ``$DatabaseName`` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;")

    $schemaSource = ([string](Resolve-Path -LiteralPath $schemaPath)).Replace('\', '/')
    $seedSource = ([string](Resolve-Path -LiteralPath $seedPath)).Replace('\', '/')

    Write-Host "Running schema.sql..."
    Invoke-MySql @("--database=$DatabaseName", "--execute=SOURCE $schemaSource")

    Write-Host "Running seed-data.sql..."
    Invoke-MySql @("--database=$DatabaseName", "--execute=SOURCE $seedSource")

    $counts = Invoke-MySqlQuery -DatabaseName $DatabaseName -Query "SELECT (SELECT COUNT(*) FROM app_user), (SELECT COUNT(*) FROM admin), (SELECT COUNT(*) FROM car_model), (SELECT COUNT(*) FROM car_param), (SELECT COUNT(*) FROM car_feature_score);"
    $parts = $counts -split "`t"
    if ($parts.Count -ge 5) {
        Write-Host "Imported rows: app_user=$($parts[0]), admin=$($parts[1]), car_model=$($parts[2]), car_param=$($parts[3]), car_feature_score=$($parts[4])"
    }

    if ($RecalculateScores) {
        $url = $BackendUrl.TrimEnd("/") + "/api/admin/cars/scores/recalculate"
        Write-Host "Calling score recalculation endpoint: $url"
        Invoke-RestMethod -Method Post -Uri $url | Out-Null
        $scoreCount = Invoke-MySqlQuery -DatabaseName $DatabaseName -Query "SELECT COUNT(*) FROM car_feature_score;"
        Write-Host "Score rows after recalculation: $scoreCount"
    }
    else {
        Write-Host "Next step: start backend and call POST /api/admin/cars/scores/recalculate, or rerun this script with -RecalculateScores after backend starts."
    }

    Write-Host "Database initialization completed."
}
finally {
    if (Test-Path -LiteralPath $defaultsFile) {
        Remove-Item -LiteralPath $defaultsFile -Force
    }
}
