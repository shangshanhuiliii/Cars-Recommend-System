package com.carsrecommend.system.mapper;

import com.carsrecommend.system.dto.CarImageAssetQuery;
import com.carsrecommend.system.entity.CarImageAsset;
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
public class CarImageAssetMapper {

    private static final String SELECT_COLUMNS = """
            id, car_id, original_filename, stored_filename, content_type, size_bytes,
            width, height, public_url, storage_path, checksum, audit_status, reject_reason,
            created_by_admin_id, reviewed_by_admin_id, deleted, create_time, update_time, review_time
            """;

    private static final RowMapper<CarImageAsset> ROW_MAPPER = (resultSet, rowNum) -> {
        CarImageAsset asset = new CarImageAsset();
        asset.setId(resultSet.getLong("id"));
        asset.setCarId(resultSet.getLong("car_id"));
        asset.setOriginalFilename(resultSet.getString("original_filename"));
        asset.setStoredFilename(resultSet.getString("stored_filename"));
        asset.setContentType(resultSet.getString("content_type"));
        asset.setSizeBytes(resultSet.getLong("size_bytes"));
        asset.setWidth(resultSet.getInt("width"));
        asset.setHeight(resultSet.getInt("height"));
        asset.setPublicUrl(resultSet.getString("public_url"));
        asset.setStoragePath(resultSet.getString("storage_path"));
        asset.setChecksum(resultSet.getString("checksum"));
        asset.setAuditStatus(resultSet.getString("audit_status"));
        asset.setRejectReason(resultSet.getString("reject_reason"));
        asset.setCreatedByAdminId(resultSet.getLong("created_by_admin_id"));
        asset.setReviewedByAdminId(readLong(resultSet, "reviewed_by_admin_id"));
        asset.setDeleted(resultSet.getBoolean("deleted"));
        asset.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        asset.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        asset.setReviewTime(readLocalDateTime(resultSet, "review_time"));
        return asset;
    };

    private final JdbcTemplate jdbcTemplate;

    public CarImageAssetMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CarImageAsset insert(CarImageAsset asset) {
        String sql = """
                INSERT INTO car_image_asset (
                    car_id, original_filename, stored_filename, content_type, size_bytes,
                    width, height, public_url, storage_path, checksum, audit_status,
                    reject_reason, created_by_admin_id, reviewed_by_admin_id, review_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            int index = 1;
            statement.setLong(index++, asset.getCarId());
            statement.setString(index++, asset.getOriginalFilename());
            statement.setString(index++, asset.getStoredFilename());
            statement.setString(index++, asset.getContentType());
            statement.setLong(index++, asset.getSizeBytes());
            statement.setInt(index++, asset.getWidth());
            statement.setInt(index++, asset.getHeight());
            statement.setString(index++, asset.getPublicUrl());
            statement.setString(index++, asset.getStoragePath());
            statement.setString(index++, asset.getChecksum());
            statement.setString(index++, asset.getAuditStatus());
            statement.setString(index++, asset.getRejectReason());
            statement.setLong(index++, asset.getCreatedByAdminId());
            statement.setObject(index++, asset.getReviewedByAdminId());
            statement.setObject(index, asset.getReviewTime());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            asset.setId(key.longValue());
        }
        return asset;
    }

    public long count(CarImageAssetQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM car_image_asset");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return total == null ? 0 : total;
    }

    public List<CarImageAsset> page(CarImageAssetQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(SELECT_COLUMNS).append(" FROM car_image_asset");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query);
        sql.append(" ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?");
        params.add(query.getSize());
        params.add((query.getPage() - 1L) * query.getSize());
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public Optional<CarImageAsset> findById(Long id) {
        List<CarImageAsset> assets = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_image_asset WHERE id = ? AND deleted = FALSE",
                ROW_MAPPER,
                id);
        return assets.stream().findFirst();
    }

    public int updateAudit(Long id, String auditStatus, String rejectReason, Long reviewedByAdminId) {
        return jdbcTemplate.update(
                """
                        UPDATE car_image_asset
                        SET audit_status = ?, reject_reason = ?, reviewed_by_admin_id = ?,
                            review_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = FALSE
                        """,
                auditStatus,
                rejectReason,
                reviewedByAdminId,
                id);
    }

    public int softDelete(Long id) {
        return jdbcTemplate.update(
                """
                        UPDATE car_image_asset
                        SET deleted = TRUE, update_time = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = FALSE
                        """,
                id);
    }

    private void appendFilters(StringBuilder sql, List<Object> params, CarImageAssetQuery query) {
        sql.append(" WHERE deleted = FALSE");
        if (query.getCarId() != null) {
            sql.append(" AND car_id = ?");
            params.add(query.getCarId());
        }
        if (StringUtils.hasText(query.getAuditStatus())) {
            sql.append(" AND audit_status = ?");
            params.add(query.getAuditStatus().trim());
        }
    }

    private static Long readLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
