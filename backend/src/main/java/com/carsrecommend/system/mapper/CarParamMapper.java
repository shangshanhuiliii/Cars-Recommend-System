package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.CarParam;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarParamMapper {

    private static final String SELECT_COLUMNS = """
            id, car_id, length_mm, width_mm, height_mm, wheelbase_mm, fuel_consumption,
            electric_consumption, electric_range_km, total_range_km, acceleration_100,
            airbag_count, has_abs, has_esp, has_active_brake, has_lane_keep,
            has_adaptive_cruise, has_blind_spot, has_reverse_camera, has_360_camera,
            has_ota, has_voice_control, has_auto_parking, screen_size, assist_drive_level,
            deleted, create_time, update_time
            """;

    private static final RowMapper<CarParam> ROW_MAPPER = (resultSet, rowNum) -> {
        CarParam param = new CarParam();
        param.setId(resultSet.getLong("id"));
        param.setCarId(resultSet.getLong("car_id"));
        param.setLengthMm(resultSet.getInt("length_mm"));
        param.setWidthMm(resultSet.getInt("width_mm"));
        param.setHeightMm(resultSet.getInt("height_mm"));
        param.setWheelbaseMm(resultSet.getInt("wheelbase_mm"));
        param.setFuelConsumption(resultSet.getBigDecimal("fuel_consumption"));
        param.setElectricConsumption(resultSet.getBigDecimal("electric_consumption"));
        param.setElectricRangeKm(readInteger(resultSet, "electric_range_km"));
        param.setTotalRangeKm(readInteger(resultSet, "total_range_km"));
        param.setAcceleration100(resultSet.getBigDecimal("acceleration_100"));
        param.setAirbagCount(resultSet.getInt("airbag_count"));
        param.setHasAbs(resultSet.getBoolean("has_abs"));
        param.setHasEsp(resultSet.getBoolean("has_esp"));
        param.setHasActiveBrake(resultSet.getBoolean("has_active_brake"));
        param.setHasLaneKeep(resultSet.getBoolean("has_lane_keep"));
        param.setHasAdaptiveCruise(resultSet.getBoolean("has_adaptive_cruise"));
        param.setHasBlindSpot(resultSet.getBoolean("has_blind_spot"));
        param.setHasReverseCamera(resultSet.getBoolean("has_reverse_camera"));
        param.setHas360Camera(resultSet.getBoolean("has_360_camera"));
        param.setHasOta(resultSet.getBoolean("has_ota"));
        param.setHasVoiceControl(resultSet.getBoolean("has_voice_control"));
        param.setHasAutoParking(resultSet.getBoolean("has_auto_parking"));
        param.setScreenSize(resultSet.getBigDecimal("screen_size"));
        param.setAssistDriveLevel(resultSet.getString("assist_drive_level"));
        param.setDeleted(resultSet.getBoolean("deleted"));
        param.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        param.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return param;
    };

    private final JdbcTemplate jdbcTemplate;

    public CarParamMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CarParam> findByCarId(Long carId) {
        List<CarParam> params = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_param WHERE car_id = ? AND deleted = FALSE",
                ROW_MAPPER,
                carId);
        return params.stream().findFirst();
    }

    public CarParam insert(CarParam param) {
        String sql = """
                INSERT INTO car_param (
                    car_id, length_mm, width_mm, height_mm, wheelbase_mm,
                    fuel_consumption, electric_consumption, electric_range_km, total_range_km,
                    acceleration_100, airbag_count, has_abs, has_esp, has_active_brake,
                    has_lane_keep, has_adaptive_cruise, has_blind_spot, has_reverse_camera,
                    has_360_camera, has_ota, has_voice_control, has_auto_parking,
                    screen_size, assist_drive_level
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            setSaveParams(statement, param, false);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            param.setId(key.longValue());
        }
        return param;
    }

    public int updateByCarId(CarParam param) {
        String sql = """
                UPDATE car_param
                SET length_mm = ?, width_mm = ?, height_mm = ?, wheelbase_mm = ?,
                    fuel_consumption = ?, electric_consumption = ?, electric_range_km = ?, total_range_km = ?,
                    acceleration_100 = ?, airbag_count = ?, has_abs = ?, has_esp = ?, has_active_brake = ?,
                    has_lane_keep = ?, has_adaptive_cruise = ?, has_blind_spot = ?, has_reverse_camera = ?,
                    has_360_camera = ?, has_ota = ?, has_voice_control = ?, has_auto_parking = ?,
                    screen_size = ?, assist_drive_level = ?, update_time = CURRENT_TIMESTAMP
                WHERE car_id = ? AND deleted = FALSE
                """;
        return jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            setSaveParams(statement, param, true);
            return statement;
        });
    }

    private static void setSaveParams(PreparedStatement statement, CarParam param, boolean update) throws SQLException {
        int index = 1;
        if (!update) {
            statement.setLong(index++, param.getCarId());
        }
        statement.setInt(index++, param.getLengthMm());
        statement.setInt(index++, param.getWidthMm());
        statement.setInt(index++, param.getHeightMm());
        statement.setInt(index++, param.getWheelbaseMm());
        statement.setBigDecimal(index++, param.getFuelConsumption());
        statement.setBigDecimal(index++, param.getElectricConsumption());
        statement.setObject(index++, param.getElectricRangeKm());
        statement.setObject(index++, param.getTotalRangeKm());
        statement.setBigDecimal(index++, param.getAcceleration100());
        statement.setInt(index++, param.getAirbagCount());
        statement.setBoolean(index++, param.getHasAbs());
        statement.setBoolean(index++, param.getHasEsp());
        statement.setBoolean(index++, param.getHasActiveBrake());
        statement.setBoolean(index++, param.getHasLaneKeep());
        statement.setBoolean(index++, param.getHasAdaptiveCruise());
        statement.setBoolean(index++, param.getHasBlindSpot());
        statement.setBoolean(index++, param.getHasReverseCamera());
        statement.setBoolean(index++, param.getHas360Camera());
        statement.setBoolean(index++, param.getHasOta());
        statement.setBoolean(index++, param.getHasVoiceControl());
        statement.setBoolean(index++, param.getHasAutoParking());
        statement.setBigDecimal(index++, param.getScreenSize());
        statement.setString(index++, param.getAssistDriveLevel());
        if (update) {
            statement.setLong(index, param.getCarId());
        }
    }

    private static Integer readInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
