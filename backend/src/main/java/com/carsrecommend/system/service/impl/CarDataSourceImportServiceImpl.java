package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.enums.EnergyType;
import com.carsrecommend.system.dto.CarDataSourceImportItem;
import com.carsrecommend.system.dto.CarParamSaveRequest;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.CarParamMapper;
import com.carsrecommend.system.service.CarDataSourceImportService;
import com.carsrecommend.system.vo.CarDataSourceImportIssueVO;
import com.carsrecommend.system.vo.CarDataSourceImportResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarDataSourceImportServiceImpl implements CarDataSourceImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final ObjectMapper objectMapper;
    private final CarModelMapper carModelMapper;
    private final CarParamMapper carParamMapper;
    private final TransactionTemplate transactionTemplate;

    public CarDataSourceImportServiceImpl(
            ObjectMapper objectMapper,
            CarModelMapper carModelMapper,
            CarParamMapper carParamMapper,
            TransactionTemplate transactionTemplate) {
        this.objectMapper = objectMapper;
        this.carModelMapper = carModelMapper;
        this.carParamMapper = carParamMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CarDataSourceImportResultVO importJson(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的数据源文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("数据源文件大小不能超过 5MB");
        }

        JsonNode carsNode = readCarsNode(file);
        CarDataSourceImportResultVO result = new CarDataSourceImportResultVO();
        result.setTotalCount(carsNode.size());
        Set<String> seenKeys = new HashSet<>();

        int rowNo = 1;
        for (Iterator<JsonNode> iterator = carsNode.elements(); iterator.hasNext(); rowNo++) {
            JsonNode rowNode = iterator.next();
            CarDataSourceImportItem item = readItem(rowNode, result, rowNo);
            if (item == null) {
                incrementFailed(result);
                continue;
            }

            normalize(item);
            String uniqueKey = uniqueKey(item);
            String validationError = validate(item);
            if (validationError != null) {
                incrementFailed(result);
                addIssue(result, rowNo, uniqueKey, "FAILED", validationError);
                continue;
            }
            if (!seenKeys.add(uniqueKey)) {
                incrementSkipped(result);
                addIssue(result, rowNo, uniqueKey, "SKIPPED", "导入文件中存在重复车型");
                continue;
            }

            try {
                Boolean created = transactionTemplate.execute(status -> upsert(item));
                if (Boolean.TRUE.equals(created)) {
                    result.setCreatedCount(result.getCreatedCount() + 1);
                } else {
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (RuntimeException ex) {
                incrementFailed(result);
                addIssue(result, rowNo, uniqueKey, "FAILED", ex.getMessage());
            }
        }
        return result;
    }

    private JsonNode readCarsNode(MultipartFile file) {
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            JsonNode carsNode = root.isArray() ? root : root.get("cars");
            if (carsNode == null || !carsNode.isArray()) {
                throw new BusinessException("JSON 根节点必须是数组，或包含 cars 数组的对象");
            }
            return carsNode;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据源 JSON 读取失败：" + ex.getMessage());
        }
    }

    private CarDataSourceImportItem readItem(
            JsonNode rowNode,
            CarDataSourceImportResultVO result,
            int rowNo) {
        try {
            return objectMapper.treeToValue(rowNode, CarDataSourceImportItem.class);
        } catch (Exception ex) {
            addIssue(result, rowNo, null, "FAILED", "行 JSON 格式无效：" + ex.getMessage());
            return null;
        }
    }

    private Boolean upsert(CarDataSourceImportItem item) {
        Optional<CarModel> existing = carModelMapper.findActiveByNaturalKey(
                item.getBrand(),
                item.getSeries(),
                item.getModelName(),
                item.getLaunchYear());
        CarModel carModel = toCarModel(item);
        boolean created = existing.isEmpty();
        if (created) {
            carModelMapper.insert(carModel);
        } else {
            carModel.setId(existing.get().getId());
            carModelMapper.update(carModel);
        }

        CarParam param = toCarParam(carModel.getId(), item.getParam());
        if (carParamMapper.findByCarId(carModel.getId()).isPresent()) {
            carParamMapper.updateByCarId(param);
        } else {
            carParamMapper.insert(param);
        }
        return created;
    }

    private void normalize(CarDataSourceImportItem item) {
        item.setBrand(trim(item.getBrand()));
        item.setSeries(trim(item.getSeries()));
        item.setModelName(trim(item.getModelName()));
        item.setImageUrl(trimToNull(item.getImageUrl()));
        if (item.getParam() != null) {
            item.getParam().setAssistDriveLevel(trimToNull(item.getParam().getAssistDriveLevel()));
        }
    }

    private String validate(CarDataSourceImportItem item) {
        if (!StringUtils.hasText(item.getBrand())) return "品牌不能为空";
        if (!StringUtils.hasText(item.getSeries())) return "车系不能为空";
        if (!StringUtils.hasText(item.getModelName())) return "车型名称不能为空";
        if (item.getGuidePrice() == null || item.getGuidePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "指导价必须大于 0";
        }
        if (item.getBodyType() == null) return "车型级别不能为空";
        if (item.getEnergyType() == null) return "动力类型不能为空";
        if (!item.getEnergyType().isCarModelType() || item.getEnergyType() == EnergyType.NEW_ENERGY) {
            return "车型动力类型必须是燃油、纯电、插混或增程";
        }
        if (item.getSeats() == null || item.getSeats() < 2 || item.getSeats() > 9) {
            return "座位数必须在 2 到 9 之间";
        }
        if (item.getLaunchYear() != null && (item.getLaunchYear() < 1990 || item.getLaunchYear() > 2100)) {
            return "上市年份必须在 1990 到 2100 之间";
        }
        if (item.getSalesVolume() == null || item.getSalesVolume() < 0) return "销量必须大于或等于 0";
        if (item.getUserRating() == null
                || item.getUserRating().compareTo(BigDecimal.ZERO) < 0
                || item.getUserRating().compareTo(BigDecimal.valueOf(5)) > 0) {
            return "口碑评分必须在 0 到 5 之间";
        }
        if (item.getAuditStatus() == null) return "审核状态不能为空";
        return validateParam(item.getParam());
    }

    private String validateParam(CarParamSaveRequest param) {
        if (param == null) return "参数信息不能为空";
        if (param.getCarId() != null) return "数据源导入时参数中不能包含 carId";
        if (!positive(param.getLengthMm())) return "车长必须为正数";
        if (!positive(param.getWidthMm())) return "车宽必须为正数";
        if (!positive(param.getHeightMm())) return "车高必须为正数";
        if (!positive(param.getWheelbaseMm())) return "轴距必须为正数";
        if (negative(param.getFuelConsumption())) return "油耗必须大于或等于 0";
        if (negative(param.getElectricConsumption())) return "电耗必须大于或等于 0";
        if (param.getElectricRangeKm() != null && param.getElectricRangeKm() < 0) return "纯电续航必须大于或等于 0";
        if (param.getTotalRangeKm() != null && param.getTotalRangeKm() < 0) return "综合续航必须大于或等于 0";
        if (negative(param.getAcceleration100())) return "百公里加速必须大于或等于 0";
        if (param.getAirbagCount() == null || param.getAirbagCount() < 0) return "气囊数量必须大于或等于 0";
        if (param.getHasAbs() == null) return "ABS 配置不能为空";
        if (param.getHasEsp() == null) return "ESP 配置不能为空";
        if (param.getHasActiveBrake() == null) return "主动刹车配置不能为空";
        if (param.getHasLaneKeep() == null) return "车道保持配置不能为空";
        if (param.getHasAdaptiveCruise() == null) return "自适应巡航配置不能为空";
        if (param.getHasBlindSpot() == null) return "并线辅助配置不能为空";
        if (param.getHasReverseCamera() == null) return "倒车影像配置不能为空";
        if (param.getHas360Camera() == null) return "360 全景配置不能为空";
        if (param.getHasOta() == null) return "OTA 配置不能为空";
        if (param.getHasVoiceControl() == null) return "语音交互配置不能为空";
        if (param.getHasAutoParking() == null) return "自动泊车配置不能为空";
        if (negative(param.getScreenSize())) return "屏幕尺寸必须大于或等于 0";
        if (param.getAssistDriveLevel() != null && param.getAssistDriveLevel().length() > 16) {
            return "辅助驾驶等级不能超过 16 个字符";
        }
        return null;
    }

    private CarModel toCarModel(CarDataSourceImportItem item) {
        CarModel carModel = new CarModel();
        carModel.setBrand(item.getBrand());
        carModel.setSeries(item.getSeries());
        carModel.setModelName(item.getModelName());
        carModel.setGuidePrice(item.getGuidePrice());
        carModel.setBodyType(item.getBodyType().getCode());
        carModel.setEnergyType(item.getEnergyType().getCode());
        carModel.setSeats(item.getSeats());
        carModel.setLaunchYear(item.getLaunchYear());
        carModel.setImageUrl(item.getImageUrl());
        carModel.setSalesVolume(item.getSalesVolume());
        carModel.setUserRating(item.getUserRating());
        carModel.setAuditStatus(item.getAuditStatus().getCode());
        return carModel;
    }

    private CarParam toCarParam(Long carId, CarParamSaveRequest request) {
        CarParam param = new CarParam();
        param.setCarId(carId);
        param.setLengthMm(request.getLengthMm());
        param.setWidthMm(request.getWidthMm());
        param.setHeightMm(request.getHeightMm());
        param.setWheelbaseMm(request.getWheelbaseMm());
        param.setFuelConsumption(request.getFuelConsumption());
        param.setElectricConsumption(request.getElectricConsumption());
        param.setElectricRangeKm(request.getElectricRangeKm());
        param.setTotalRangeKm(request.getTotalRangeKm());
        param.setAcceleration100(request.getAcceleration100());
        param.setAirbagCount(request.getAirbagCount());
        param.setHasAbs(request.getHasAbs());
        param.setHasEsp(request.getHasEsp());
        param.setHasActiveBrake(request.getHasActiveBrake());
        param.setHasLaneKeep(request.getHasLaneKeep());
        param.setHasAdaptiveCruise(request.getHasAdaptiveCruise());
        param.setHasBlindSpot(request.getHasBlindSpot());
        param.setHasReverseCamera(request.getHasReverseCamera());
        param.setHas360Camera(request.getHas360Camera());
        param.setHasOta(request.getHasOta());
        param.setHasVoiceControl(request.getHasVoiceControl());
        param.setHasAutoParking(request.getHasAutoParking());
        param.setScreenSize(request.getScreenSize());
        param.setAssistDriveLevel(request.getAssistDriveLevel());
        return param;
    }

    private void incrementFailed(CarDataSourceImportResultVO result) {
        result.setFailedCount(result.getFailedCount() + 1);
    }

    private void incrementSkipped(CarDataSourceImportResultVO result) {
        result.setSkippedCount(result.getSkippedCount() + 1);
    }

    private void addIssue(
            CarDataSourceImportResultVO result,
            Integer rowNo,
            String uniqueKey,
            String type,
            String message) {
        CarDataSourceImportIssueVO issue = new CarDataSourceImportIssueVO();
        issue.setRowNo(rowNo);
        issue.setUniqueKey(uniqueKey);
        issue.setType(type);
        issue.setMessage(message);
        result.getIssues().add(issue);
    }

    private String uniqueKey(CarDataSourceImportItem item) {
        return "%s|%s|%s|%s".formatted(
                safeKeyPart(item.getBrand()),
                safeKeyPart(item.getSeries()),
                safeKeyPart(item.getModelName()),
                item.getLaunchYear() == null ? "NULL" : item.getLaunchYear().toString());
    }

    private String safeKeyPart(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private boolean negative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
