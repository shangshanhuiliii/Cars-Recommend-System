package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.AppUser;
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
public class AppUserMapper {

    private static final String SELECT_COLUMNS = """
            id, username, password, nickname, phone, deleted, create_time, update_time
            """;

    private static final RowMapper<AppUser> ROW_MAPPER = (resultSet, rowNum) -> {
        AppUser user = new AppUser();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setNickname(resultSet.getString("nickname"));
        user.setPhone(resultSet.getString("phone"));
        user.setDeleted(resultSet.getBoolean("deleted"));
        user.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        user.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return user;
    };

    private final JdbcTemplate jdbcTemplate;

    public AppUserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppUser> findActiveByUsername(String username) {
        List<AppUser> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM app_user WHERE username = ? AND deleted = FALSE",
                ROW_MAPPER,
                username);
        return users.stream().findFirst();
    }

    public Optional<AppUser> findActiveById(Long id) {
        List<AppUser> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM app_user WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return users.stream().findFirst();
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
