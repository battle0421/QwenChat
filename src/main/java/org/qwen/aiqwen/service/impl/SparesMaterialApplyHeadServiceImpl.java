package org.qwen.aiqwen.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.dto.SparesMaterialApplyHeadDto;
import org.qwen.aiqwen.entity.SparesMaterialApplyHead;
import org.qwen.aiqwen.exception.BusinessException;
import org.qwen.aiqwen.repository.SparesMaterialApplyHeadRepository;
import org.qwen.aiqwen.service.SparesMaterialApplyHeadService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SparesMaterialApplyHeadServiceImpl implements SparesMaterialApplyHeadService {

    @Autowired
    private SparesMaterialApplyHeadRepository repository;

    @Override
    @Transactional
    public SparesMaterialApplyHeadDto create(SparesMaterialApplyHeadDto dto) {
        SparesMaterialApplyHead entity = new SparesMaterialApplyHead();
        BeanUtils.copyProperties(dto, entity);
        entity.setDelFlag(0);

        SparesMaterialApplyHead saved = repository.save(entity);

        SparesMaterialApplyHeadDto result = new SparesMaterialApplyHeadDto();
        BeanUtils.copyProperties(saved, result);

        log.info("创建物料申请成功，ID: {}", saved.getId());
        return result;
    }

    @Override
    @Transactional
    public SparesMaterialApplyHeadDto update(Long id, SparesMaterialApplyHeadDto dto) {
        SparesMaterialApplyHead entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在，ID: " + id));

        if (entity.getDelFlag() == 1) {
            throw new BusinessException("记录已删除，无法更新");
        }

        BeanUtils.copyProperties(dto, entity, "id", "createTime", "updateTime");

        SparesMaterialApplyHead updated = repository.save(entity);

        SparesMaterialApplyHeadDto result = new SparesMaterialApplyHeadDto();
        BeanUtils.copyProperties(updated, result);

        log.info("更新物料申请成功，ID: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SparesMaterialApplyHead entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在，ID: " + id));

        if (entity.getDelFlag() == 1) {
            log.warn("记录已删除，ID: {}", id);
            return;
        }

        int rows = repository.logicDeleteById(id);
        if (rows == 0) {
            throw new BusinessException("删除失败");
        }

        log.info("逻辑删除物料申请成功，ID: {}", id);
    }

    @Override
    public SparesMaterialApplyHeadDto getById(Long id) {
        SparesMaterialApplyHead entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在，ID: " + id));

        if (entity.getDelFlag() == 1) {
            throw new BusinessException("记录已删除");
        }

        SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    @Override
    public SparesMaterialApplyHeadDto getByRepairId(String repairId) {
        SparesMaterialApplyHead entity = repository.findByRepairIdAndDelFlag(repairId, 0)
                .orElseThrow(() -> new BusinessException("记录不存在，repairId: " + repairId));

        SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    @Override
    public List<SparesMaterialApplyHeadDto> getByMaterialNumber(String materialNumber) {
        List<SparesMaterialApplyHead> entities = repository.findByMaterialNumberAndDelFlag(materialNumber, 0);

        return entities.stream().map(entity -> {
            SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<SparesMaterialApplyHeadDto> getPage(Pageable pageable) {
        Page<SparesMaterialApplyHead> page = repository.findByDelFlag(0, pageable);

        return page.map(entity -> {
            SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        });
    }

    @Override
    public Page<SparesMaterialApplyHeadDto> getByBillUserCode(String billUserCode, Pageable pageable) {
        Page<SparesMaterialApplyHead> page = repository.findByBillUserCodeAndDelFlag(billUserCode, 0, pageable);

        return page.map(entity -> {
            SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        });
    }
}
