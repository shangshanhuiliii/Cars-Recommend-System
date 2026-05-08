package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.AppUser;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AppUserMapper {

    private static final String SELECT_COLUMNS = """
            id, username, password, nickname, phone, status, deleted, create_time, update_time
            """;

    private static final RowMapper<AppUser> ROW_MAPPER = (resultSet, rowNum) -> {
        AppUser user = new AppUser();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setNickname(resultSet.getString("nickname"));
        user.setPhone(resultSet.getString("phone"));
        user.setStatus(resultSet.getString("status"));
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
                "SELECT " + SELECT_COLUMNS
                        + " FROM app_user WHERE username = ? AND status = 'ACTIVE' AND deleted = FALSE",
                ROW_MAPPER,
                username);
        return users.stream().findFirst();
    }

    public Optional<AppUser> findActiveById(Long id) {
        List<AppUser> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM app_user WHERE id = ? AND status = 'ACTIVE' AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return users.stream().findFirst();
    }

    public Optional<AppUser> findByIdForAdmin(Long id) {
        List<AppUser> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM app_user WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return users.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE username = ?",
                Long.class,
                username);
        return count != null && count > 0;
    }

    public AppUser insert(AppUser user) {
        String sql = """
                INSERT INTO app_user (username, password, nickname, phone, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getNickname());
            statement.setString(4, user.getPhone());
            statement.setString(5, user.getStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
    }

    public long countForAdmin(String keyword, String status) {
        QueryParts queryParts = buildAdminFilter(keyword, status);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE deleted = FALSE" + queryParts.whereClause(),
                Long.class,
                queryParts.args().toArray());
        return total == null ? 0 : total;
    }

    public List<AppUser> findPageForAdmin(String keyword, String status, long limit, long offset) {
        QueryParts queryParts = buildAdminFilter(keyword, status);
        List<Object> args = new ArrayList<>(queryParts.args());
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM app_user WHERE deleted = FALSE"
                        + queryParts.whereClause()
                        + " ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?",
                ROW_MAPPER,
                args.toArray());
    }

    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update(
                """
                        UPDATE app_user
                        SET status = ?, update_time = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = FALSE
                        """,
                status,
                id);
    }

    private QueryParts buildAdminFilter(String keyword, String status) {
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (LOWER(username) LIKE ? OR LOWER(COALESCE(nickname, '')) LIKE ? OR phone LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add("%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        return new QueryParts(where.toString(), args);
    }

    private record QueryParts(String whereClause, List<Object> args) {
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
