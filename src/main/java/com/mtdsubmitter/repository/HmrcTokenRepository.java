package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.HmrcToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HmrcTokenRepository extends JpaRepository<HmrcToken, UUID> {
    Optional<HmrcToken> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
