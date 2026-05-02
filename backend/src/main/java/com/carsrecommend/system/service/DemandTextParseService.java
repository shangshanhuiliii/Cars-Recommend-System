package com.carsrecommend.system.service;

import com.carsrecommend.system.dto.DemandTextParseRequest;
import com.carsrecommend.system.vo.DemandTextParseVO;

public interface DemandTextParseService {

    DemandTextParseVO parse(DemandTextParseRequest request);
}
