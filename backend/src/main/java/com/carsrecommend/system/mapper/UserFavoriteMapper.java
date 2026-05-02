package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.UserFavorite;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserFavoriteMapper {

    private static final RowMapper<UserFavorite> ROW_MAPPER = (resultSet, rowNum) -> {
        UserFavorite favorite = new UserFavorite();
        favorite.setId(resultSet.getLong("id"));
        favorite.setUserId(resultSet.getLong("user_id"));
        favorite.setCarId(resultSet.getLong("car_id"));
        favorite.setDeleted(resultSet.getBoolean("deleted"));
        favorite.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        favorite.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return favorite;
    };

    private static final RowMapper<UserFavoriteItemVO> ITEM_ROW_MAPPER = (resultSet, rowNum) -> {
        UserFavoriteItemVO item = new UserFavoriteItemVO();
        item.setFavoriteId(resultSet.getLong("favorite_id"));
        item.setCarId(resultSet.getLong("car_id"));
        item.setBrand(resultSet.getString("brand"));
        item.setSeries(resultSet.getString("series"));
        item.setModelName(resultSet.getString("model_name"));
        item.setGuidePrice(resultSet.getBigDecimal("guide_price"));
        item.setBodyType(resultSet.getString("body_type"));
        item.setEnergyType(resultSet.getString("energy_type"));
        item.setSeats(resultSet.getInt("seats"));
        item.setImageUrl(resultSet.getString("image_url"));
        item.setFavoriteTime(readLocalDateTime(resultSet, "favorite_time"));
        item.setScoreSummary(scoreSummary(resultSet));
        return item;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserFavoriteMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserFavorite> findAnyByUserIdAndCarId(Long userId, Long carId) {
        List<UserFavorite> favorites = jdbcTemplate.query(
                """
                        SELECT id, user_id, car_id, deleted, create_time, update_time
                        FROM user_favorite
                        WHERE user_id = ? AND car_id = ?
                        """,
                ROW_MAPPER,
                userId,
                carId);
        return favorites.stream().findFirst();
    }

    public int insert(Long userId, Long carId) {
        return jdbcTemplate.update(
                "INSERT INTO user_favorite (user_id, car_id) VALUES (?, ?)",
                userId,
                carId);
    }

    public int activate(Long userId, Long carId) {
        return jdbcTemplate.update(
                """
                        UPDATE user_favorite
                        SET deleted = FALSE, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND car_id = ?
                        """,
                userId,
                carId);
    }

    public int cancel(Long userId, Long carId) {
        return jdbcTemplate.update(
                """
                        UPDATE user_favorite
                        SET deleted = TRUE, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND car_id = ? AND deleted = FALSE
                        """,
                userId,
                carId);
    }

    public boolean existsActive(Long userId, Long carId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_favorite
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
                        FROM user_favorite uf
                        JOIN car_model cm ON cm.id = uf.car_id AND cm.deleted = FALSE
                        WHERE uf.user_id = ? AND uf.deleted = FALSE
                        """,
                Long.class,
                userId);
        return count == null ? 0 : count;
    }

    public List<UserFavoriteItemVO> findPageByUserId(Long userId, long limit, long offset) {
        return jdbcTemplate.query(
                """
                        SELECT uf.id AS favorite_id, uf.create_time AS favorite_time,
                               cm.id AS car_id, cm.brand, cm.series, cm.model_name, cm.guide_price,
                               cm.body_type, cm.energy_type, cm.seats, cm.image_url,
                               cfs.space_score, cfs.safety_score, cfs.energy_score,
                               cfs.intelligence_score, cfs.comfort_score, cfs.power_score,
                               cfs.reputation_score, cfs.popularity_score
                        FROM user_favorite uf
                        JOIN car_model cm ON cm.id = uf.car_id AND cm.deleted = FALSE
                        LEFT JOIN car_feature_score cfs ON cfs.car_id = cm.id AND cfs.deleted = FALSE
                        WHERE uf.user_id = ? AND uf.deleted = FALSE
                        ORDER BY uf.update_time DESC, uf.id DESC
                        LIMIT ? OFFSET ?
                        """,
                ITEM_ROW_MAPPER,
                userId,
                limit,
                offset);
    }

    private static Map<String, java.math.BigDecimal> scoreSummary(ResultSet resultSet) throws SQLException {
        if (resultSet.getBigDecimal("space_score") == null) {
            return null;
        }
        Map<String, java.math.BigDecimal> scores = new LinkedHashMap<>();
        scores.put("space", resultSet.getBigDecimal("space_score"));
        scores.put("safety", resultSet.getBigDecimal("safety_score"));
        scores.put("energy", resultSet.getBigDecimal("energy_score"));
        scores.put("intelligence", resultSet.getBigDecimal("intelligence_score"));
        scores.put("comfort", resultSet.getBigDecimal("comfort_score"));
        scores.put("power", resultSet.getBigDecimal("power_score"));
        scores.put("reputation", resultSet.getBigDecimal("reputation_score"));
        scores.put("popularity", resultSet.getBigDecimal("popularity_score"));
        return scores;
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
