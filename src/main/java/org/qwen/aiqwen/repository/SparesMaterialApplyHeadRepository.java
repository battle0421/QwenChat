package org.qwen.aiqwen.repository;

import org.qwen.aiqwen.entity.SparesMaterialApplyHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparesMaterialApplyHeadRepository extends JpaRepository<SparesMaterialApplyHead, Long> {

    Optional<SparesMaterialApplyHead> findByRepairIdAndDelFlag(String repairId, Integer delFlag);

    Optional<SparesMaterialApplyHead> findByRepairSnAndDelFlag(String repairSn, Integer delFlag);

    List<SparesMaterialApplyHead> findByMaterialNumberAndDelFlag(String materialNumber, Integer delFlag);

    Page<SparesMaterialApplyHead> findByDelFlag(Integer delFlag, Pageable pageable);

    Page<SparesMaterialApplyHead> findByBillUserCodeAndDelFlag(String billUserCode, Integer delFlag, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE SparesMaterialApplyHead s SET s.delFlag = 1 WHERE s.id = :id")
    int logicDeleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE SparesMaterialApplyHead s SET s.delFlag = 1 WHERE s.repairId = :repairId")
    int logicDeleteByRepairId(@Param("repairId") String repairId);
}
