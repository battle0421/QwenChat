package org.qwen.aiqwen.skill.tool;


import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.dto.SparesMaterialApplyHeadDto;
import org.qwen.aiqwen.service.SparesMaterialApplyHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SparesMaterialApplyTool {

    @Autowired
    private SparesMaterialApplyHeadService sparesMaterialApplyHeadService;

    @Tool("""
            创建新的备件物料申请单。
            当用户需要申请新的备件物料时调用此工具。
            需要提供维修单号、物料编号等关键信息。
            """)
    public String createSparesMaterialApply(
            @P("维修单ID，必填") String repairId,
            @P("维修单序列号，必填") String repairSn,
            @P("物料编号，必填") String materialNumber,
            @P("仓库编码，可选") String warehouseCode,
            @P("仓库名称，可选") String warehouseName,
            @P("设备型号，可选") String deviceModel,
            @P("库存组织编码，可选") String stockOrgCode,
            @P("申请人编码，可选") String billUserCode,
            @P("申请人姓名，可选") String billUserName,
            @P("申请部门编码，可选") String billDeptCode,
            @P("申请部门名称，可选") String billDeptName,
            @P("维修单号，可选") String repairNo
    ) {
        try {
            log.info("通过 MCP Tool 创建领料单 - repairId: {}, materialNumber: {}", repairId, materialNumber);

            SparesMaterialApplyHeadDto dto = new SparesMaterialApplyHeadDto();
            dto.setRepairId(repairId);
            dto.setRepairSn(repairSn);
            dto.setMaterialNumber(materialNumber);
            dto.setWarehouseCode(warehouseCode);
            dto.setWarehouseName(warehouseName);
            dto.setDeviceModel(deviceModel);
            dto.setStockOrgCode(stockOrgCode);
            dto.setBillUserCode(billUserCode);
            dto.setBillUserName(billUserName);
            dto.setBillDeptCode(billDeptCode);
            dto.setBillDeptName(billDeptName);
            dto.setRepairNo(repairNo);

            SparesMaterialApplyHeadDto result = sparesMaterialApplyHeadService.create(dto);

            return String.format("领料单创建成功！申请单ID: %s, 维修单ID: %s, 物料编号: %s",
                    result.getId(), result.getRepairId(), result.getMaterialNumber());

        } catch (Exception e) {
            log.error("创建领料单失败", e);
            return "创建领料单失败: " + e.getMessage();
        }
    }
}