package com.cloudship.repository;

import com.cloudship.entity.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {

    Optional<GitRepository> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
