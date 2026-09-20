package com.cloudship.repository;

import com.cloudship.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    long countByProjectId(Long projectId);
}
