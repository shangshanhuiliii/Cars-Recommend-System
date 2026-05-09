package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.dto.DemandTextParseRequest;
import com.carsrecommend.system.service.DemandTextParseService;
import com.carsrecommend.system.vo.DemandTextParseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DemandTextParseServiceImpl implements DemandTextParseService {

    private static final long DEFAULT_SEED_USER_ID = 1L;
    private static final String NUMBER = "([0-9]+(?:\\.[0-9]+)?|[零〇一二两三四五六七八九十百]+)";
    private static final Pattern BUDGET_RANGE = Pattern.compile(
            NUMBER + "\\s*(?:万|万元|w)?\\s*(?:-|—|－|~|～|到|至)\\s*" + NUMBER + "\\s*(?:万|万元|w)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUDGET_MAX = Pattern.compile(
            "(?:(?:不超过|不要超过|不高于|最高|最多|控制在|预算(?:在)?|价位(?:在)?|价格(?:在)?)\\s*)?"
                    + NUMBER + "\\s*(?:万|万元|w)\\s*(?:以内|以下|之内|内|封顶)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUDGET_MAX_CONTEXT = Pattern.compile(
            "(?:预算|价位|价格)\\s*(?:在|大概|大约|约)?\\s*" + NUMBER + "\\s*(?:万|万元|w)\\s*(?:左右)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUDGET_MIN = Pattern.compile(
            NUMBER + "\\s*(?:万|万元|w)\\s*(?:以上|起|往上)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MIN_SEATS = Pattern.compile(
            "(?:至少|不少于|不低于|最低|要|需要|希望|想要)?\\s*([2-9]|二|两|三|四|五|六|七|八|九)\\s*座(?:以上|起)?");

    private static final List<ValueRule> BODY_RULES = List.of(
            new ValueRule("SUV", List.of("suv", "越野", "城市越野")),
            new ValueRule("轿车", List.of("轿车", "小轿车", "三厢车")),
            new ValueRule("MPV", List.of("mpv", "商务车", "保姆车")));

    private static final List<ValueRule> ENERGY_RULES = List.of(
            new ValueRule("燃油", List.of("燃油", "油车", "汽油", "燃油车")),
            new ValueRule("纯电", List.of("纯电", "电车", "电动", "纯电动", "ev")),
            new ValueRule("插混", List.of("插混", "插电混动", "插电式混动", "phev", "混动")),
            new ValueRule("增程", List.of("增程", "增程式")),
            new ValueRule("新能源", List.of("新能源")));

    private static final List<ValueRule> SCENE_RULES = List.of(
            new ValueRule("城市通勤", List.of("通勤", "上下班", "市区", "城市", "代步")),
            new ValueRule("家庭出行", List.of("家用", "家庭", "带娃", "孩子", "老人", "一家人")),
            new ValueRule("长途自驾", List.of("长途", "自驾", "高速", "旅行", "出游")),
            new ValueRule("新手代步", List.of("新手", "练手", "好开", "停车方便")),
            new ValueRule("商务接待", List.of("商务", "接待", "客户", "体面")));

    private static final List<FactorRule> FACTOR_RULES = List.of(
            new FactorRule("price", "价格", List.of("性价比", "便宜", "省钱", "价格低", "预算有限", "划算")),
            new FactorRule("space", "空间", List.of("空间大", "大空间", "宽敞", "后排大", "后备箱", "装载")),
            new FactorRule("safety", "安全", List.of("安全", "安全性", "主动安全", "气囊", "刹车")),
            new FactorRule("energy", "能耗", List.of("省油", "油耗低", "能耗低", "续航", "电耗低", "省电")),
            new FactorRule("intelligence", "智能", List.of("智能", "车机", "辅助驾驶", "自动驾驶", "智驾")),
            new FactorRule("comfort", "舒适", List.of("舒适", "舒服", "静音", "座椅", "减震")),
            new FactorRule("power", "动力", List.of("动力强", "动力", "加速", "性能", "操控")),
            new FactorRule("reputation", "口碑", List.of("口碑", "质量", "可靠", "耐用", "保值")),
            new FactorRule("popularity", "热度", List.of("热门", "销量", "保有量", "市场热度")));

    private static final List<String> DIMENSION_ORDER = List.of(
            "price",
            "space",
            "safety",
            "energy",
            "intelligence",
            "comfort",
            "power",
            "reputation",
            "popularity");

    private static final List<String> UNSUPPORTED_TERMS = List.of(
            "二手",
            "贷款",
            "分期",
            "颜色",
            "外观颜色",
            "保险",
            "上牌",
            "现车");

    @Override
    public DemandTextParseVO parse(DemandTextParseRequest request) {
        String rawText = request.text().trim();
        String normalized = rawText.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");

        BudgetRange budgetRange = parseBudget(normalized);
        List<String> bodyTypes = extractValues(normalized, BODY_RULES);
        List<String> energyTypes = extractValues(normalized, ENERGY_RULES);
        Integer minSeats = parseMinSeats(normalized);
        List<String> scenes = extractValues(normalized, SCENE_RULES);
        Map<String, Integer> factorWeights = parseFactorWeights(normalized);
        List<String> unsupportedTerms = extractUnsupportedTerms(normalized);
        List<String> ambiguousTerms = extractAmbiguousTerms(normalized);
        BigDecimal confidenceScore = calculateConfidence(
                budgetRange,
                bodyTypes,
                energyTypes,
                minSeats,
                scenes,
                factorWeights,
                unsupportedTerms,
                ambiguousTerms);

        return new DemandTextParseVO(
                resolveUserId(request.userId()),
                rawText,
                budgetRange.min(),
                budgetRange.max(),
                bodyTypes,
                energyTypes,
                minSeats,
                scenes.isEmpty() ? List.of("综合需求") : scenes,
                factorWeights,
                List.of(),
                List.of(),
                buildProfileText(budgetRange, bodyTypes, energyTypes, minSeats, scenes, factorWeights),
                unsupportedTerms,
                ambiguousTerms,
                confidenceScore);
    }

    private Long resolveUserId(Long userId) {
        Long currentUserId = AuthContext.currentUserIdOrNull();
        return currentUserId != null ? currentUserId : (userId == null ? DEFAULT_SEED_USER_ID : userId);
    }

    private BudgetRange parseBudget(String text) {
        Matcher rangeMatcher = BUDGET_RANGE.matcher(text);
        if (rangeMatcher.find()) {
            BigDecimal min = toYuan(rangeMatcher.group(1));
            BigDecimal max = toYuan(rangeMatcher.group(2));
            if (min != null && max != null && min.compareTo(max) > 0) {
                return new BudgetRange(max, min);
            }
            return new BudgetRange(min, max);
        }

        BigDecimal max = firstBudgetValue(BUDGET_MAX, text);
        if (max == null) {
            max = firstBudgetValue(BUDGET_MAX_CONTEXT, text);
        }
        BigDecimal min = firstBudgetValue(BUDGET_MIN, text);
        return new BudgetRange(min, max);
    }

    private BigDecimal firstBudgetValue(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        for (int index = 1; index <= matcher.groupCount(); index++) {
            String value = matcher.group(index);
            if (value != null) {
                return toYuan(value);
            }
        }
        return null;
    }

    private BigDecimal toYuan(String value) {
        BigDecimal wan = parseNumber(value);
        if (wan == null) {
            return null;
        }
        return wan.multiply(new BigDecimal("10000")).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.matches("[0-9]+(?:\\.[0-9]+)?")) {
            return new BigDecimal(value);
        }
        return BigDecimal.valueOf(parseChineseNumber(value));
    }

    private int parseChineseNumber(String value) {
        if (value.contains("百")) {
            String[] parts = value.split("百", -1);
            int hundred = parts[0].isBlank() ? 1 : singleChineseNumber(parts[0]);
            int rest = parts.length > 1 && !parts[1].isBlank() ? parseChineseNumber(parts[1]) : 0;
            return hundred * 100 + rest;
        }
        if (value.contains("十")) {
            String[] parts = value.split("十", -1);
            int ten = parts[0].isBlank() ? 1 : singleChineseNumber(parts[0]);
            int rest = parts.length > 1 && !parts[1].isBlank() ? singleChineseNumber(parts[1]) : 0;
            return ten * 10 + rest;
        }
        return singleChineseNumber(value);
    }

    private int singleChineseNumber(String value) {
        int total = 0;
        for (char item : value.toCharArray()) {
            total = total * 10 + switch (item) {
                case '一' -> 1;
                case '二', '两' -> 2;
                case '三' -> 3;
                case '四' -> 4;
                case '五' -> 5;
                case '六' -> 6;
                case '七' -> 7;
                case '八' -> 8;
                case '九' -> 9;
                case '零', '〇' -> 0;
                default -> 0;
            };
        }
        return total;
    }

    private Integer parseMinSeats(String text) {
        Matcher matcher = MIN_SEATS.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        BigDecimal value = parseNumber(matcher.group(1));
        return value == null ? null : value.intValue();
    }

    private List<String> extractValues(String text, List<ValueRule> rules) {
        Set<String> result = new LinkedHashSet<>();
        for (ValueRule rule : rules) {
            if (containsAny(text, rule.terms())) {
                result.add(rule.value());
            }
        }
        return List.copyOf(result);
    }

    private Map<String, Integer> parseFactorWeights(String text) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String dimension : DIMENSION_ORDER) {
            values.put(dimension, 0);
        }
        for (FactorRule rule : FACTOR_RULES) {
            int hitCount = countTermHits(text, rule.terms());
            if (hitCount > 0) {
                values.put(rule.dimension(), Math.min(10, 7 + hitCount));
            }
        }
        return values;
    }

    private int countTermHits(String text, List<String> terms) {
        int total = 0;
        for (String term : terms) {
            if (text.contains(term.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))) {
                total++;
            }
        }
        return total;
    }

    private boolean containsAny(String text, List<String> terms) {
        return terms.stream()
                .map(term -> term.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))
                .anyMatch(text::contains);
    }

    private List<String> extractUnsupportedTerms(String text) {
        return UNSUPPORTED_TERMS.stream()
                .filter(text::contains)
                .toList();
    }

    private List<String> extractAmbiguousTerms(String text) {
        List<String> terms = new ArrayList<>();
        if (text.contains("混动") && !text.contains("插混") && !text.contains("插电混动") && !text.contains("插电式混动")) {
            terms.add("混动");
        }
        if (text.contains("好看") || text.contains("外观")) {
            terms.add("外观偏好");
        }
        return terms;
    }

    private BigDecimal calculateConfidence(
            BudgetRange budgetRange,
            List<String> bodyTypes,
            List<String> energyTypes,
            Integer minSeats,
            List<String> scenes,
            Map<String, Integer> factorWeights,
            List<String> unsupportedTerms,
            List<String> ambiguousTerms) {
        int recognized = 0;
        if (budgetRange.min() != null || budgetRange.max() != null) {
            recognized++;
        }
        if (!bodyTypes.isEmpty()) {
            recognized++;
        }
        if (!energyTypes.isEmpty()) {
            recognized++;
        }
        if (minSeats != null) {
            recognized++;
        }
        if (!scenes.isEmpty()) {
            recognized++;
        }
        if (factorWeights.values().stream().anyMatch(value -> value > 0)) {
            recognized++;
        }
        BigDecimal confidence = new BigDecimal("0.35")
                .add(new BigDecimal("0.10").multiply(BigDecimal.valueOf(recognized)))
                .subtract(new BigDecimal("0.03").multiply(BigDecimal.valueOf(unsupportedTerms.size())))
                .subtract(new BigDecimal("0.05").multiply(BigDecimal.valueOf(ambiguousTerms.size())));
        if (confidence.compareTo(new BigDecimal("0.95")) > 0) {
            confidence = new BigDecimal("0.95");
        }
        if (confidence.compareTo(new BigDecimal("0.25")) < 0) {
            confidence = new BigDecimal("0.25");
        }
        return confidence.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildProfileText(
            BudgetRange budgetRange,
            List<String> bodyTypes,
            List<String> energyTypes,
            Integer minSeats,
            List<String> scenes,
            Map<String, Integer> factorWeights) {
        List<String> parts = new ArrayList<>();
        parts.add("自然语言解析草稿");
        parts.add(buildBudgetText(budgetRange));
        if (!bodyTypes.isEmpty()) {
            parts.add("可接受" + joinChinese(bodyTypes));
        }
        if (!energyTypes.isEmpty()) {
            parts.add("可接受" + joinChinese(energyTypes) + "动力");
        }
        if (minSeats != null) {
            parts.add("最低" + minSeats + "座");
        }
        parts.add("使用场景为" + joinChinese(scenes.isEmpty() ? List.of("综合需求") : scenes));

        List<String> highFactors = FACTOR_RULES.stream()
                .filter(rule -> factorWeights.getOrDefault(rule.dimension(), 0) > 0)
                .sorted((left, right) -> factorWeights.get(right.dimension()).compareTo(factorWeights.get(left.dimension())))
                .limit(4)
                .map(FactorRule::label)
                .toList();
        parts.add(highFactors.isEmpty() ? "偏好权重待用户确认" : "重点关注" + joinChinese(highFactors));
        return String.join("，", parts) + "。";
    }

    private String buildBudgetText(BudgetRange budgetRange) {
        if (budgetRange.min() != null && budgetRange.max() != null) {
            return "预算" + formatWan(budgetRange.min()) + "-" + formatWan(budgetRange.max()) + "万";
        }
        if (budgetRange.max() != null) {
            return "预算" + formatWan(budgetRange.max()) + "万以内";
        }
        if (budgetRange.min() != null) {
            return "预算" + formatWan(budgetRange.min()) + "万以上";
        }
        return "预算未识别";
    }

    private String formatWan(BigDecimal price) {
        return price.divide(new BigDecimal("10000"), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String joinChinese(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        if (values.size() == 2) {
            return values.get(0) + "和" + values.get(1);
        }
        List<String> head = values.subList(0, values.size() - 1);
        return String.join("、", head) + "和" + values.get(values.size() - 1);
    }

    private record ValueRule(String value, List<String> terms) {
    }

    private record FactorRule(String dimension, String label, List<String> terms) {
    }

    private record BudgetRange(BigDecimal min, BigDecimal max) {
    }
}
