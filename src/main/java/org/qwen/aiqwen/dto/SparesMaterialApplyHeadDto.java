package org.qwen.aiqwen.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SparesMaterialApplyHeadDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String repairId;
    private String repairSn;
    private String warehouseCode;
    private String warehouseName;
    private String materialNumber;
    private String deviceModel;
    private String stockOrgCode;
    private String billUserCode;
    private String billUserName;
    private String billDeptCode;
    private String billDeptName;
    private String repairNo;
}
