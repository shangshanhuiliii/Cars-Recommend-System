package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.UserDemand;
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
public class UserDemandMapper {

    private static final String SELECT_COLUMNS = """
            id, user_id, raw_text, budget_min, budget_max, body_type, energy_type, seats,
            scene, focus_factors, excluded_brands, excluded_car_ids, profile_text,
            weight_price, weight_space, weight_safety, weight_energy, weight_intelligence,
            weight_comfort, weight_power, weight_reputation, weight_popularity,
            deleted, create_time, update_time
            """;

    private static final RowMapper<UserDemand> ROW_MAPPER = (resultSet, rowNum) -> {
        UserDemand demand = new UserDemand();
        demand.setId(resultSet.getLong("id"));
        demand.setUserId(resultSet.getLong("user_id"));
        demand.setRawText(resultSet.getString("raw_text"));
        demand.setBudgetMin(resultSet.getBigDecimal("budget_min"));
        demand.setBudgetMax(resultSet.getBigDecimal("budget_max"));
        demand.setBodyType(resultSet.getString("body_type"));
        demand.setEnergyType(resultSet.getString("energy_type"));
        demand.setSeats(readInteger(resultSet, "seats"));
        demand.setScene(resultSet.getString("scene"));
        demand.setFocusFactors(resultSet.getString("focus_factors"));
        demand.setExcludedBrands(resultSet.getString("excluded_brands"));
        demand.setExcludedCarIds(resultSet.getString("excluded_car_ids"));
        demand.setProfileText(resultSet.getString("profile_text"));
        demand.setWeightPrice(resultSet.getBigDecimal("weight_price"));
        demand.setWeightSpace(resultSet.getBigDecimal("weight_space"));
        demand.setWeightSafety(resultSet.getBigDecimal("weight_safety"));
        demand.setWeightEnergy(resultSet.getBigDecimal("weight_energy"));
        demand.setWeightIntelligence(resultSet.getBigDecimal("weight_intelligence"));
        demand.setWeightComfort(resultSet.getBigDecimal("weight_comfort"));
        demand.setWeightPower(resultSet.getBigDecimal("weight_power"));
        demand.setWeightReputation(resultSet.getBigDecimal("weight_reputation"));
        demand.setWeightPopularity(resultSet.getBigDecimal("weight_popularity"));
        demand.setDeleted(resultSet.getBoolean("deleted"));
        demand.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        demand.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return demand;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserDemandMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsActiveUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE id = ? AND deleted = FALSE",
                Long.class,
                userId);
        return count != null && count > 0;
    }

    public UserDemand insert(UserDemand demand) {
        String sql = """
                INSERT INTO user_demand (
                    user_id, raw_text, budget_min, budget_max, body_type, energy_type, seats,
                    scene, focus_factors, excluded_brands, excluded_car_ids, profile_text,
                    weight_price, weight_space, weight_safety, weight_energy, weight_intelligence,
                    weight_comfort, weight_power, weight_reputation, weight_popularity
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            int index = 1;
            statement.setLong(index++, demand.getUserId());
            statement.setString(index++, demand.getRawText());
            statement.setBigDecimal(index++, demand.getBudgetMin());
            statement.setBigDecimal(index++, demand.getBudgetMax());
            statement.setString(index++, demand.getBodyType());
            statement.setString(index++, demand.getEnergyType());
            statement.setObject(index++, demand.getSeats());
            statement.setString(index++, demand.getScene());
            statement.setString(index++, demand.getFocusFactors());
            statement.setString(index++, demand.getExcludedBrands());
            statement.setString(index++, demand.getExcludedCarIds());
            statement.setString(index++, demand.getProfileText());
            statement.setBigDecimal(index++, demand.getWeightPrice());
            statement.setBigDecimal(index++, demand.getWeightSpace());
            statement.setBigDecimal(index++, demand.getWeightSafety());
            statement.setBigDecimal(index++, demand.getWeightEnergy());
            statement.setBigDecimal(index++, demand.getWeightIntelligence());
            statement.setBigDecimal(index++, demand.getWeightComfort());
            statement.setBigDecimal(index++, demand.getWeightPower());
            statement.setBigDecimal(index++, demand.getWeightReputation());
            statement.setBigDecimal(index, demand.getWeightPopularity());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            demand.setId(key.longValue());
        }
        return demand;
    }

    public Optional<UserDemand> findById(Long id) {
        List<UserDemand> demands = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM user_demand WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return demands.stream().findFirst();
    }

    public Optional<UserDemand> findLatestByUserId(Long userId) {
        List<UserDemand> demands = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM user_demand WHERE user_id = ? AND deleted = FALSE ORDER BY create_time DESC, id DESC LIMIT 1",
                ROW_MAPPER,
                userId);
        return demands.stream().findFirst();
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
