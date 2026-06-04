package com.carsrecommend.system.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecommendationExplanationService {

    private static final BigDecimal GOOD_SCORE_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal TAG_SCORE_THRESHOLD = new BigDecimal("85");
    private static final BigDecimal PRICE_TAG_THRESHOLD = new BigDecimal("90");
    private static final BigDecimal WEAK_SCORE_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal GAP_THRESHOLD = new BigDecimal("0.0001");
    private static final int CONTRIBUTION_REASON_LIMIT = 3;
    private static final int HIGH_WEIGHT_DIMENSION_LIMIT = 4;
    private static final String DEFAULT_REASON_TEXT =
            "该车型在多个关键维度上表现较均衡，可作为备选车型进一步对比";
    private static final String DEFAULT_WEAKNESS_TEXT = "该车型整体匹配较均衡，暂无明显短板";

    public Explanation generate(
            RecommendationScoreVector scoreVector,
            TopsisRanker.TopsisItemResult topsisResult,
            Map<String, BigDecimal> finalWeight) {
        List<DimensionExplanation> dimensions = dimensions(scoreVector, topsisResult, finalWeight);
        return new Explanation(
                generateTags(dimensions, topsisResult.totalScore()),
                generateReasonText(dimensions),
                generateWeaknessText(dimensions));
    }

    private List<String> generateTags(List<DimensionExplanation> dimensions, BigDecimal totalScore) {
        List<TagCandidate> candidates = new ArrayList<>();
        for (DimensionExplanation dimension : dimensions) {
            BigDecimal threshold = "price".equals(dimension.key()) ? PRICE_TAG_THRESHOLD : TAG_SCORE_THRESHOLD;
            if (dimension.score().compareTo(threshold) >= 0) {
                candidates.add(new TagCandidate(dimension.tag(), dimension.score()));
            }
        }

        if (totalScore != null && totalScore.compareTo(new BigDecimal("80")) >= 0) {
            candidates.add(new TagCandidate("接近理想车型", totalScore));
        }
        long balancedDimensionCount = dimensions.stream()
                .filter(dimension -> dimension.score().compareTo(new BigDecimal("75")) >= 0)
                .count();
        if (balancedDimensionCount >= 5) {
            candidates.add(new TagCandidate("多维表现均衡", new BigDecimal("76")));
        }

        if (candidates.isEmpty()) {
            return List.of("多维表现均衡");
        }
        candidates.sort(Comparator.comparing(TagCandidate::score).reversed());
        Set<String> tags = new LinkedHashSet<>();
        for (TagCandidate candidate : candidates) {
            tags.add(candidate.label());
            if (tags.size() >= 3) {
                break;
            }
        }
        return List.copyOf(tags);
    }

    private String generateReasonText(List<DimensionExplanation> dimensions) {
        List<String> reasons = dimensions.stream()
                .filter(dimension -> dimension.score().compareTo(GOOD_SCORE_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(DimensionExplanation::contribution).reversed())
                .limit(CONTRIBUTION_REASON_LIMIT)
                .map(DimensionExplanation::reasonText)
                .map(this::withoutTrailingChinesePeriod)
                .toList();
        if (reasons.size() < 2) {
            return DEFAULT_REASON_TEXT;
        }
        return String.join("；", reasons);
    }

    private String generateWeaknessText(List<DimensionExplanation> dimensions) {
        List<String> weaknesses = dimensions.stream()
                .sorted(Comparator.comparing(DimensionExplanation::weight).reversed())
                .limit(HIGH_WEIGHT_DIMENSION_LIMIT)
                .filter(dimension -> dimension.gap().compareTo(GAP_THRESHOLD) > 0)
                .filter(dimension -> dimension.score().compareTo(WEAK_SCORE_THRESHOLD) < 0)
                .sorted(Comparator.comparing(DimensionExplanation::gap).reversed())
                .limit(2)
                .map(DimensionExplanation::weaknessText)
                .map(this::withoutTrailingChinesePeriod)
                .toList();
        return weaknesses.isEmpty() ? DEFAULT_WEAKNESS_TEXT : String.join("；", weaknesses);
    }

    private String withoutTrailingChinesePeriod(String text) {
        if (text == null || !text.endsWith("。")) {
            return text;
        }
        return text.substring(0, text.length() - 1);
    }

    private List<DimensionExplanation> dimensions(
            RecommendationScoreVector scoreVector,
            TopsisRanker.TopsisItemResult topsisResult,
            Map<String, BigDecimal> finalWeight) {
        List<DimensionExplanation> dimensions = new ArrayList<>();
        for (RecommendationDimension dimension : RecommendationDimension.ORDERED) {
            String key = dimension.key();
            BigDecimal contribution = value(topsisResult.weightedNormalizedScore().get(key));
            BigDecimal gap = value(topsisResult.positiveIdeal().get(key)).subtract(contribution).max(BigDecimal.ZERO);
            dimensions.add(template(
                    key,
                    dimension.score(scoreVector),
                    value(finalWeight.get(key)),
                    contribution,
                    gap));
        }
        return dimensions;
    }

    private DimensionExplanation template(
            String key,
            BigDecimal score,
            BigDecimal weight,
            BigDecimal contribution,
            BigDecimal gap) {
        return switch (key) {
            case "price" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "价格匹配度高",
                    "该车型价格与您的预算匹配度较高，有助于控制购车成本。",
                    "该车型价格匹配度与当前候选中的理想表现仍有差距，建议结合预算弹性继续比较。");
            case "space" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "空间优秀",
                    "该车型空间表现对综合匹配贡献较高，适合家庭出行和多人乘坐场景。",
                    "该车型空间表现与理想候选仍有差距，如果您经常满载或重视乘坐宽敞度，需要重点对比。");
            case "safety" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "安全配置高",
                    "该车型安全配置表现对推荐结果贡献较高，符合您对安全性的关注。",
                    "该车型安全配置与理想候选仍有差距，如果您重视主动安全和安全气囊配置，需要谨慎比较。");
            case "energy" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "能耗表现好",
                    "该车型能耗和续航表现贡献较高，日常用车成本更有优势。",
                    "该车型能耗或续航表现与理想候选仍有差距，如果您关注长期用车成本，需要继续对比。");
            case "intelligence" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "智能配置丰富",
                    "该车型智能配置对综合匹配贡献较高，能提升车机体验和辅助驾驶便利性。",
                    "该车型智能配置与理想候选仍有差距，如果您看重车机体验和辅助驾驶，可继续对比其他车型。");
            case "comfort" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "舒适性较好",
                    "该车型舒适性表现对推荐结果贡献较高，适合日常通勤和长途乘坐。",
                    "该车型舒适性与理想候选仍有差距，如果您重视乘坐体验，需要关注空间、配置和口碑差异。");
            case "power" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "动力表现强",
                    "该车型动力表现对综合匹配贡献较高，适合看重加速和驾驶响应的用户。",
                    "该车型动力表现与理想候选仍有差距，如果您经常高速或满载出行，需要重点试驾确认。");
            case "reputation" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "口碑较好",
                    "该车型口碑评分对推荐结果贡献较高，用户评价和可靠性表现更稳。",
                    "该车型口碑表现与理想候选仍有差距，建议结合真实车主评价和售后表现继续判断。");
            case "popularity" -> new DimensionExplanation(
                    key,
                    value(score),
                    weight,
                    contribution,
                    gap,
                    "热门车型",
                    "该车型市场热度对推荐结果贡献较高，销量基础和关注度表现较好。",
                    "该车型市场热度与理想候选仍有差距，可结合保有量和后续用车便利性继续判断。");
            default -> throw new IllegalArgumentException("unsupported recommendation dimension: " + key);
        };
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Explanation(
            List<String> tags,
            String reasonText,
            String weaknessText) {
    }

    private record DimensionExplanation(
            String key,
            BigDecimal score,
            BigDecimal weight,
            BigDecimal contribution,
            BigDecimal gap,
            String tag,
            String reasonText,
            String weaknessText) {
    }

    private record TagCandidate(String label, BigDecimal score) {
    }
}
