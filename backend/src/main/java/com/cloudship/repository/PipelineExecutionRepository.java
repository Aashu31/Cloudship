package com.cloudship.repository;

import com.cloudship.entity.PipelineExecution;
import com.cloudship.entity.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, Long> {

    List<PipelineExecution> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<PipelineExecution> findAllByOrderByCreatedAtDesc();

    List<PipelineExecution> findTop10ByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PipelineExecution p WHERE p.status IN ('QUEUED', 'CHECKING_OUT', 'BUILDING', 'PACKAGING', 'PUSHING_IMAGE', 'VERIFYING_IMAGE', 'DEPLOYING_TO_AKS', 'VERIFYING_ROLLOUT')")
    long countActive();

    Optional<PipelineExecution> findByCiBuildId(Long ciBuildId);

    Optional<PipelineExecution> findByDeploymentId(Long deploymentId);

    Optional<PipelineExecution> findFirstByProjectIdAndStatusInOrderByCreatedAtDesc(
            Long projectId, Collection<PipelineStatus> statuses);

    Optional<PipelineExecution> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);
}
