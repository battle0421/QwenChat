package org.qwen.aiqwen.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;
import org.qwen.aiqwen.dto.SparesMaterialApplyHeadDto;
import org.qwen.aiqwen.entity.SparesMaterialApplyHead;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SparesMaterialApplyAddHeadSkill implements ParentSkill{

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String supportIntent() {
        return "add_sparesMaterialApplyHead";
    }

    /**
     * 添加物料申请单
     * @param memoryId
     * @param intent
     * @return
     */
    @Override
    public Result<Object> execute(String memoryId,IntentResultAiDto intent) {
        log.info("保存成功：{}", intent);
        // 直接从 slots 取！
        String itemName = intent.getString("itemName");
        // 获取槽位信息
        Map<String, Object> slots = intent.getSlots();

        // 转换为实体对象
        SparesMaterialApplyHead entity = convertSlotsToEntity(slots);

        // 设置默认值
        entity.setDelFlag(0);

        // 调用 Service 保存
        SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
        BeanUtils.copyProperties(entity, dto);

        return  Result.success(dto);


    }

    private SparesMaterialApplyHead convertSlotsToEntity(Map<String, Object> slots) {
        try {
            String json = objectMapper.writeValueAsString(slots);
            return objectMapper.readValue(json, SparesMaterialApplyHead.class);
        } catch (Exception e) {
            log.error("转换失败", e);
            throw new RuntimeException("槽位信息转换失败", e);
        }
    }
}
