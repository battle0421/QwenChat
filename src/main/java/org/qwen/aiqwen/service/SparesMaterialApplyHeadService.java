package org.qwen.aiqwen.service;

import org.qwen.aiqwen.dto.SparesMaterialApplyHeadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SparesMaterialApplyHeadService {

    SparesMaterialApplyHeadDto create(SparesMaterialApplyHeadDto dto);

    SparesMaterialApplyHeadDto update(Long id, SparesMaterialApplyHeadDto dto);

    void delete(Long id);

    SparesMaterialApplyHeadDto getById(Long id);

    SparesMaterialApplyHeadDto getByRepairId(String repairId);

    List<SparesMaterialApplyHeadDto> getByMaterialNumber(String materialNumber);

    Page<SparesMaterialApplyHeadDto> getPage(Pageable pageable);

    Page<SparesMaterialApplyHeadDto> getByBillUserCode(String billUserCode, Pageable pageable);
}
