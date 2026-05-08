package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.UserCompareCar;
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
public class UserCompareCarMapper {

    private static final RowMapper<UserCompareCar> ROW_MAPPER = (resultSet, rowNum) -> {
        UserCompareCar item = new UserCompareCar();
        item.setId(resultSet.getLong("id"));
        item.setUserId(resultSet.getLong("user_id"));
        item.setCarId(resultSet.getLong("car_id"));
        item.setSortNo(resultSet.getInt("sort_no"));
        item.setDeleted(resultSet.getBoolean("deleted"));
        item.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        item.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return item;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserCompareCarMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserCompareCar> findAnyByUserIdAndCarId(Long userId, Long carId) {
        List<UserCompareCar> rows = jdbcTemplate.query(
                """
                        SELECT id, user_id, car_id, sort_no, deleted, create_time, update_time
                        FROM user_compare_car
                        WHERE user_id = ? AND car_id = ?
                        """,
                ROW_MAPPER,
                userId,
                carId);
        return rows.stream().findFirst();
    }

    public boolean existsActive(Long userId, Long carId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_compare_car
                        WHERE user_id = ? AND car_id = ? AND deleted = FALSE
                        """,
                Long.class,
                userId,
                carId);
        return count != null && count > 0;
    }

    public long countActiveByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_compare_car ucc
                        JOIN car_model cm ON cm.id = ucc.car_id AND cm.deleted = FALSE
                        WHERE ucc.user_id = ? AND ucc.deleted = FALSE
                        """,
                Long.class,
                userId);
        return count == null ? 0 : count;
    }

    public Integer nextSortNo(Long userId) {
        Integer value = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(sort_no), -1) + 1
                        FROM user_compare_car
                        WHERE user_id = ? AND deleted = FALSE
                        """,
                Integer.class,
                userId);
        return value == null ? 0 : value;
    }

    public List<Long> findActiveCarIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT ucc.car_id
                        FROM user_compare_car ucc
                        JOIN car_model cm ON cm.id = ucc.car_id AND cm.deleted = FALSE
                        WHERE ucc.user_id = ? AND ucc.deleted = FALSE
                        ORDER BY ucc.sort_no ASC, ucc.update_time ASC, ucc.create_time ASC, ucc.id ASC
                        """,
                Long.class,
                userId);
    }

    public int insert(Long userId, Long carId, Integer sortNo) {
        return jdbcTemplate.update(
                """
                        INSERT INTO user_compare_car (user_id, car_id, sort_no)
                        VALUES (?, ?, ?)
                        """,
                userId,
                carId,
                sortNo);
    }

    public int activate(Long userId, Long carId, Integer sortNo) {
        return jdbcTemplate.update(
                """
                        UPDATE user_compare_car
                        SET deleted = FALSE, sort_no = ?, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND car_id = ?
                        """,
                sortNo,
                userId,
                carId);
    }

    public int softDelete(Long userId, Long carId) {
        return jdbcTemplate.update(
                """
                        UPDATE user_compare_car
                        SET deleted = TRUE, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND car_id = ? AND deleted = FALSE
                        """,
                userId,
                carId);
    }

    public int clear(Long userId) {
        return jdbcTemplate.update(
                """
                        UPDATE user_compare_car
                        SET deleted = TRUE, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND deleted = FALSE
                        """,
                userId);
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
