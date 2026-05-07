package com.carsrecommend.system.mapper;

import com.carsrecommend.system.dto.CarPageQuery;
import com.carsrecommend.system.entity.CarModel;
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
public class CarModelMapper {

    private static final String SELECT_COLUMNS = """
            id, brand, series, model_name, guide_price, body_type, energy_type, seats,
            launch_year, image_url, sales_volume, user_rating, audit_status,
            deleted, create_time, update_time
            """;

    private static final RowMapper<CarModel> ROW_MAPPER = (resultSet, rowNum) -> {
        CarModel carModel = new CarModel();
        carModel.setId(resultSet.getLong("id"));
        carModel.setBrand(resultSet.getString("brand"));
        carModel.setSeries(resultSet.getString("series"));
        carModel.setModelName(resultSet.getString("model_name"));
        carModel.setGuidePrice(resultSet.getBigDecimal("guide_price"));
        carModel.setBodyType(resultSet.getString("body_type"));
        carModel.setEnergyType(resultSet.getString("energy_type"));
        carModel.setSeats(resultSet.getInt("seats"));
        carModel.setLaunchYear(readInteger(resultSet, "launch_year"));
        carModel.setImageUrl(resultSet.getString("image_url"));
        carModel.setSalesVolume(resultSet.getInt("sales_volume"));
        carModel.setUserRating(resultSet.getBigDecimal("user_rating"));
        carModel.setAuditStatus(resultSet.getString("audit_status"));
        carModel.setDeleted(resultSet.getBoolean("deleted"));
        carModel.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        carModel.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return carModel;
    };

    private final JdbcTemplate jdbcTemplate;

    public CarModelMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(CarPageQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM car_model");
        List<Object> params = new ArrayList<>();
        appendPageFilters(sql, params, query);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return total == null ? 0 : total;
    }

    public List<CarModel> page(CarPageQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(SELECT_COLUMNS).append(" FROM car_model");
        List<Object> params = new ArrayList<>();
        appendPageFilters(sql, params, query);
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(query.getSize());
        params.add((query.getPage() - 1L) * query.getSize());
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public Optional<CarModel> findById(Long id) {
        List<CarModel> cars = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_model WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return cars.stream().findFirst();
    }

    public List<CarModel> findAllActive() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_model WHERE deleted = FALSE ORDER BY id ASC",
                ROW_MAPPER);
    }

    public boolean existsActiveById(Long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_model WHERE id = ? AND deleted = FALSE",
                Long.class,
                id);
        return count != null && count > 0;
    }

    public List<CarModel> findApprovedRecommendationCandidates() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM car_model WHERE audit_status = 'APPROVED' AND deleted = FALSE ORDER BY id ASC",
                ROW_MAPPER);
    }

    public List<String> findActiveBrands() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT brand FROM car_model WHERE deleted = FALSE ORDER BY brand ASC",
                String.class);
    }

    public List<CarModel> findActiveOptions(String keyword, int limit) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(SELECT_COLUMNS).append(" FROM car_model WHERE deleted = FALSE");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String value = "%" + keyword.trim() + "%";
            sql.append(" AND (brand LIKE ? OR series LIKE ? OR model_name LIKE ?)");
            params.add(value);
            params.add(value);
            params.add(value);
        }
        sql.append(" ORDER BY id ASC LIMIT ?");
        params.add(limit);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public int findMaxSalesVolume() {
        Integer maxSales = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sales_volume), 0) FROM car_model WHERE deleted = FALSE",
                Integer.class);
        return maxSales == null ? 0 : maxSales;
    }

    public CarModel insert(CarModel carModel) {
        String sql = """
                INSERT INTO car_model (
                    brand, series, model_name, guide_price, body_type, energy_type,
                    seats, launch_year, image_url, sales_volume, user_rating, audit_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            int index = 1;
            statement.setString(index++, carModel.getBrand());
            statement.setString(index++, carModel.getSeries());
            statement.setString(index++, carModel.getModelName());
            statement.setBigDecimal(index++, carModel.getGuidePrice());
            statement.setString(index++, carModel.getBodyType());
            statement.setString(index++, carModel.getEnergyType());
            statement.setInt(index++, carModel.getSeats());
            statement.setObject(index++, carModel.getLaunchYear());
            statement.setString(index++, carModel.getImageUrl());
            statement.setInt(index++, carModel.getSalesVolume());
            statement.setBigDecimal(index++, carModel.getUserRating());
            statement.setString(index, carModel.getAuditStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            carModel.setId(key.longValue());
        }
        return carModel;
    }

    public int update(CarModel carModel) {
        String sql = """
                UPDATE car_model
                SET brand = ?, series = ?, model_name = ?, guide_price = ?, body_type = ?,
                    energy_type = ?, seats = ?, launch_year = ?, image_url = ?, sales_volume = ?,
                    user_rating = ?, audit_status = ?, update_time = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = FALSE
                """;
        return jdbcTemplate.update(
                sql,
                carModel.getBrand(),
                carModel.getSeries(),
                carModel.getModelName(),
                carModel.getGuidePrice(),
                carModel.getBodyType(),
                carModel.getEnergyType(),
                carModel.getSeats(),
                carModel.getLaunchYear(),
                carModel.getImageUrl(),
                carModel.getSalesVolume(),
                carModel.getUserRating(),
                carModel.getAuditStatus(),
                carModel.getId());
    }

    public int updateImageUrl(Long id, String imageUrl) {
        return jdbcTemplate.update(
                """
                        UPDATE car_model
                        SET image_url = ?, update_time = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = FALSE
                        """,
                imageUrl,
                id);
    }

    public int softDelete(Long id) {
        return jdbcTemplate.update(
                "UPDATE car_model SET deleted = TRUE, update_time = CURRENT_TIMESTAMP WHERE id = ? AND deleted = FALSE",
                id);
    }

    private void appendPageFilters(StringBuilder sql, List<Object> params, CarPageQuery query) {
        sql.append(" WHERE deleted = FALSE");
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = "%" + query.getKeyword().trim() + "%";
            sql.append(" AND (brand LIKE ? OR series LIKE ? OR model_name LIKE ?)");
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }
        if (StringUtils.hasText(query.getBodyType())) {
            sql.append(" AND body_type = ?");
            params.add(query.getBodyType().trim());
        }
        if (StringUtils.hasText(query.getEnergyType())) {
            sql.append(" AND energy_type = ?");
            params.add(query.getEnergyType().trim());
        }
        if (StringUtils.hasText(query.getAuditStatus())) {
            sql.append(" AND audit_status = ?");
            params.add(query.getAuditStatus().trim());
        }
        if (query.getMinPrice() != null) {
            sql.append(" AND guide_price >= ?");
            params.add(query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            sql.append(" AND guide_price <= ?");
            params.add(query.getMaxPrice());
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
