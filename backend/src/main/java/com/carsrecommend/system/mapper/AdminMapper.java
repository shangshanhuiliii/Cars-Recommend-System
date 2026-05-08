package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.Admin;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminMapper {

    private static final String SELECT_COLUMNS = """
            id, username, password, role, deleted, create_time, update_time
            """;

    private static final RowMapper<Admin> ROW_MAPPER = (resultSet, rowNum) -> {
        Admin admin = new Admin();
        admin.setId(resultSet.getLong("id"));
        admin.setUsername(resultSet.getString("username"));
        admin.setPassword(resultSet.getString("password"));
        admin.setRole(resultSet.getString("role"));
        admin.setDeleted(resultSet.getBoolean("deleted"));
        admin.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        admin.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return admin;
    };

    private final JdbcTemplate jdbcTemplate;

    public AdminMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Admin> findActiveByUsername(String username) {
        List<Admin> admins = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM admin WHERE username = ? AND deleted = FALSE",
                ROW_MAPPER,
                username);
        return admins.stream().findFirst();
    }

    public Optional<Admin> findActiveById(Long id) {
        List<Admin> admins = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM admin WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return admins.stream().findFirst();
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
