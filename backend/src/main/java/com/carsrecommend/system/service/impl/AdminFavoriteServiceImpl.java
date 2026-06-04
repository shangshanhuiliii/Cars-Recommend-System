package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.service.AdminFavoriteService;
import com.carsrecommend.system.vo.AdminFavoriteCarVO;
import com.carsrecommend.system.vo.AdminFavoriteUserVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminFavoriteServiceImpl implements AdminFavoriteService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final AppUserMapper appUserMapper;
    private final CarModelMapper carModelMapper;

    public AdminFavoriteServiceImpl(
            JdbcTemplate jdbcTemplate,
            AppUserMapper appUserMapper,
            CarModelMapper carModelMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.appUserMapper = appUserMapper;
        this.carModelMapper = carModelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminFavoriteCarVO> cars(Integer page, Integer size, String keyword, Long userId) {
        if (userId != null && appUserMapper.findByIdForAdmin(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        QueryParts queryParts = buildFavoriteCarFilter(keyword, userId);
        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM (
                            SELECT cm.id
                            FROM user_favorite uf
                            JOIN car_model cm ON cm.id = uf.car_id AND cm.deleted = FALSE
                        """
                        + queryParts.whereClause()
                        + """
                            GROUP BY cm.id
                        ) favorite_cars
                        """,
                Long.class,
                queryParts.args().toArray());
        List<Object> args = new ArrayList<>(queryParts.args());
        args.add(pageSize);
        args.add((long) (pageNo - 1) * pageSize);
        List<AdminFavoriteCarVO> records = jdbcTemplate.query(
                """
                        SELECT cm.id AS car_id, cm.brand, cm.series, cm.model_name, cm.guide_price,
                               cm.body_type, cm.energy_type, cm.image_url,
                               COUNT(uf.id) AS favorite_count,
                               MAX(uf.update_time) AS latest_favorite_time
                        FROM user_favorite uf
                        JOIN car_model cm ON cm.id = uf.car_id AND cm.deleted = FALSE
                        """
                        + queryParts.whereClause()
                        + """
                        GROUP BY cm.id, cm.brand, cm.series, cm.model_name, cm.guide_price,
                                 cm.body_type, cm.energy_type, cm.image_url
                        ORDER BY favorite_count DESC, latest_favorite_time DESC, cm.id ASC
                        LIMIT ? OFFSET ?
                        """,
                (resultSet, rowNum) -> toFavoriteCarVO(resultSet),
                args.toArray());
        return PageResult.of(records, total == null ? 0 : total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminFavoriteUserVO> users(Long carId, Integer page, Integer size) {
        if (carId == null || carId <= 0 || !carModelMapper.existsActiveById(carId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "车型不存在");
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_favorite uf
                        JOIN app_user au ON au.id = uf.user_id AND au.deleted = FALSE
                        WHERE uf.car_id = ? AND uf.deleted = FALSE
                        """,
                Long.class,
                carId);
        List<AdminFavoriteUserVO> records = jdbcTemplate.query(
                """
                        SELECT au.id AS user_id, au.username, au.nickname, au.phone, au.status,
                               uf.update_time AS favorite_time
                        FROM user_favorite uf
                        JOIN app_user au ON au.id = uf.user_id AND au.deleted = FALSE
                        WHERE uf.car_id = ? AND uf.deleted = FALSE
                        ORDER BY uf.update_time DESC, uf.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (resultSet, rowNum) -> toFavoriteUserVO(resultSet),
                carId,
                pageSize,
                (long) (pageNo - 1) * pageSize);
        return PageResult.of(records, total == null ? 0 : total, pageNo, pageSize);
    }

    private QueryParts buildFavoriteCarFilter(String keyword, Long userId) {
        StringBuilder where = new StringBuilder(" WHERE uf.deleted = FALSE ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            where.append(" AND (LOWER(cm.brand) LIKE ? OR LOWER(cm.series) LIKE ? OR LOWER(cm.model_name) LIKE ?)");
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (userId != null) {
            where.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM user_favorite filter_uf
                        WHERE filter_uf.car_id = cm.id
                          AND filter_uf.user_id = ?
                          AND filter_uf.deleted = FALSE
                    )
                    """);
            args.add(userId);
        }
        return new QueryParts(where.toString(), args);
    }

    private AdminFavoriteCarVO toFavoriteCarVO(ResultSet resultSet) throws SQLException {
        AdminFavoriteCarVO vo = new AdminFavoriteCarVO();
        vo.setCarId(resultSet.getLong("car_id"));
        vo.setBrand(resultSet.getString("brand"));
        vo.setSeries(resultSet.getString("series"));
        vo.setModelName(resultSet.getString("model_name"));
        vo.setGuidePrice(resultSet.getBigDecimal("guide_price"));
        vo.setBodyType(resultSet.getString("body_type"));
        vo.setEnergyType(resultSet.getString("energy_type"));
        vo.setImageUrl(resultSet.getString("image_url"));
        vo.setFavoriteCount(resultSet.getLong("favorite_count"));
        vo.setLatestFavoriteTime(readLocalDateTime(resultSet, "latest_favorite_time"));
        return vo;
    }

    private AdminFavoriteUserVO toFavoriteUserVO(ResultSet resultSet) throws SQLException {
        AdminFavoriteUserVO vo = new AdminFavoriteUserVO();
        vo.setUserId(resultSet.getLong("user_id"));
        vo.setUsername(resultSet.getString("username"));
        vo.setNickname(resultSet.getString("nickname"));
        vo.setPhone(resultSet.getString("phone"));
        vo.setStatus(resultSet.getString("status"));
        vo.setFavoriteTime(readLocalDateTime(resultSet, "favorite_time"));
        return vo;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException("页码必须大于或等于 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 100 之间");
        }
        return size;
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record QueryParts(String whereClause, List<Object> args) {
    }
}
