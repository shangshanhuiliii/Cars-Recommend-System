package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarDataSourceImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface CarDataSourceImportService {

    CarDataSourceImportResultVO importJson(MultipartFile file);
}
