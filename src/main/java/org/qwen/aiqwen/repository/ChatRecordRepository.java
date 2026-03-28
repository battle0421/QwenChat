
package org.qwen.aiqwen.repository;

import org.qwen.aiqwen.entity.ChatRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatRecordRepository extends JpaRepository<ChatRecord, Long> {

    Page<ChatRecord> findByStatus(String status, Pageable pageable);

    List<ChatRecord> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT c FROM ChatRecord c WHERE c.message LIKE %:keyword% OR c.response LIKE %:keyword%")
    Page<ChatRecord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}