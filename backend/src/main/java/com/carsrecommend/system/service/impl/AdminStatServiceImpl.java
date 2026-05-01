package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.service.AdminStatService;
import com.carsrecommend.system.vo.AdminStatOverviewVO;
import com.carsrecommend.system.vo.StatItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminStatServiceImpl implements AdminStatService {

    private static final List<String> BUDGET_BUCKETS = List.of(
            "8万以内", "8-12万", "10-15万", "15-25万", "25万以上", "未填写预算");
    private static final Map<String, String> FACTOR_LABELS = Map.of(
            "price", "价格",
            "space", "空间",
            "safety", "安全",
            "energy", "能耗",
            "intelligence", "智能",
            "comfort", "舒适",
            "power", "动力",
            "reputation", "口碑",
            "popularity", "热度");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminStatServiceImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatOverviewVO overview() {
        AdminStatOverviewVO vo = new AdminStatOverviewVO();
        vo.setBudgetDistribution(budgetDistribution());
        vo.setSceneDistribution(groupDemandArrayColumn("scenes"));
        vo.setFocusFactorDistribution(factorWeightDistribution());
        vo.setPopularCars(popularCars());
        vo.setRecommendStatusDistribution(groupRecommendStatus());
        vo.setEnergyTypeDistribution(groupDemandArrayColumn("energy_types"));
        vo.setBodyTypeDistribution(groupDemandArrayColumn("body_types"));
        vo.setSatisfactionDistribution(List.of());
        vo.setFeedbackReasonDistribution(List.of());
        return vo;
    }

    private List<StatItemVO> budgetDistribution() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT budget_min, budget_max FROM user_demand WHERE deleted = FALSE");
        Map<String, Long> counts = new LinkedHashMap<>();
        BUDGET_BUCKETS.forEach(bucket -> counts.put(bucket, 0L));
        for (Map<String, Object> row : rows) {
            BigDecimal budgetMin = toBigDecimal(row.get("budget_min"));
            BigDecimal budgetMax = toBigDecimal(row.get("budget_max"));
            String bucket = budgetBucket(budgetMin, budgetMax);
            counts.put(bucket, counts.getOrDefault(bucket, 0L) + 1);
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new StatItemVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private String budgetBucket(BigDecimal budgetMin, BigDecimal budgetMax) {
        BigDecimal budget = budgetMax != null ? budgetMax : budgetMin;
        if (budget == null) {
            return "未填写预算";
        }
        if (budget.compareTo(new BigDecimal("80000")) <= 0) {
            return "8万以内";
        }
        if (budget.compareTo(new BigDecimal("120000")) <= 0) {
            return "8-12万";
        }
        if (budget.compareTo(new BigDecimal("150000")) <= 0) {
            return "10-15万";
        }
        if (budget.compareTo(new BigDecimal("250000")) <= 0) {
            return "15-25万";
        }
        return "25万以上";
    }

    private List<StatItemVO> groupDemandArrayColumn(String column) {
        List<String> jsonValues = jdbcTemplate.queryForList(
                "SELECT " + column + " FROM user_demand WHERE deleted = FALSE",
                String.class);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String json : jsonValues) {
            for (String value : readStringList(json)) {
                counts.put(value, counts.getOrDefault(value, 0L) + 1);
            }
        }
        return sortedStatItems(counts);
    }

    private List<StatItemVO> groupRecommendStatus() {
        return jdbcTemplate.query(
                """
                        SELECT recommend_status AS name, COUNT(*) AS item_count
                        FROM recommend_record
                        WHERE deleted = FALSE
                        GROUP BY recommend_status
                        ORDER BY item_count DESC, name ASC
                        """,
                (resultSet, rowNum) -> new StatItemVO(resultSet.getString("name"), resultSet.getLong("item_count")));
    }

    private List<StatItemVO> popularCars() {
        return jdbcTemplate.query(
                """
                        SELECT CONCAT(cm.brand, ' ', cm.model_name) AS name, COUNT(*) AS item_count
                        FROM recommend_item ri
                        JOIN car_model cm ON cm.id = ri.car_id
                        WHERE ri.deleted = FALSE
                        GROUP BY cm.id, cm.brand, cm.model_name
                        ORDER BY item_count DESC, MIN(ri.rank_no) ASC, cm.id ASC
                        LIMIT 10
                        """,
                (resultSet, rowNum) -> new StatItemVO(resultSet.getString("name"), resultSet.getLong("item_count")));
    }

    private List<StatItemVO> factorWeightDistribution() {
        List<String> jsonValues = jdbcTemplate.queryForList(
                "SELECT factor_weights FROM user_demand WHERE deleted = FALSE",
                String.class);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String json : jsonValues) {
            JsonNode node = readJsonNode(json);
            if (!node.isObject()) {
                continue;
            }
            node.fields().forEachRemaining(entry -> {
                if (entry.getValue().asInt(0) > 0) {
                    String label = FACTOR_LABELS.getOrDefault(entry.getKey(), entry.getKey());
                    counts.put(label, counts.getOrDefault(label, 0L) + 1);
                }
            });
        }
        return sortedStatItems(counts);
    }

    private List<StatItemVO> sortedStatItems(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()) && entry.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new StatItemVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<String> readStringList(String json) {
        JsonNode node = readJsonNode(json);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private JsonNode readJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return node;
        } catch (JsonProcessingException exception) {
            return objectMapper.createArrayNode();
        }
    }
}
