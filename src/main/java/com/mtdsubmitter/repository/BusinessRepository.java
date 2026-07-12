package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {
    List<Business> findByUserId(UUID userId);
    List<Business> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<Business> findByIdAndUserId(UUID id, UUID userId);
}
