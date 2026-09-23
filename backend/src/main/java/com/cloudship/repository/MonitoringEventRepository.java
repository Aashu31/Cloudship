package com.cloudship.repository;

import com.cloudship.entity.MonitoringEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {

    List<MonitoringEvent> findTop50ByOrderByCreatedAtDesc();

    List<MonitoringEvent> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    List<MonitoringEvent> findByOrderByCreatedAtDesc(Pageable pageable);

    long countBySeverity(String severity);

    long countByCreatedAtAfter(OffsetDateTime since);
}
