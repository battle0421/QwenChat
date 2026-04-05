package org.qwen.aiqwen.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spares_material_apply_head")
public class SparesMaterialApplyHead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repair_id", nullable = false, length = 50)
    private String repairId;

    @Column(name = "repair_sn", nullable = false, length = 50)
    private String repairSn;

    @Column(name = "warehouse_code", length = 20)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 100)
    private String warehouseName;

    @Column(name = "material_number", nullable = false, length = 50)
    private String materialNumber;

    @Column(name = "device_model", length = 250)
    private String deviceModel;

    @Column(name = "stock_org_code", length = 20)
    private String stockOrgCode;

    @Column(name = "bill_user_code", length = 20)
    private String billUserCode;

    @Column(name = "bill_user_name", length = 50)
    private String billUserName;

    @Column(name = "bill_dept_code", length = 20)
    private String billDeptCode;

    @Column(name = "bill_dept_name", length = 100)
    private String billDeptName;

    @Column(name = "repair_no", length = 50)
    private String repairNo;
    @Column(name = "del_flag", length = 50)
    private int delFlag;

}
