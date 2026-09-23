package com.cloudship.repository;

import com.cloudship.entity.CIBuild;
import com.cloudship.entity.CIPushStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CIBuildRepository extends JpaRepository<CIBuild, Long> {

    List<CIBuild> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<CIBuild> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<CIBuild> findByIdAndProjectId(Long id, Long projectId);

    Optional<CIBuild> findFirstByProjectIdAndPushStatusOrderByCreatedAtDesc(Long projectId, CIPushStatus pushStatus);

    List<CIBuild> findTop10ByOrderByCreatedAtDesc();
}
