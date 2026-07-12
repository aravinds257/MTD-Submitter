package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.FinalDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinalDeclarationRepository extends JpaRepository<FinalDeclaration, UUID> {
    Optional<FinalDeclaration> findByUserIdAndTaxYearId(UUID userId, Integer taxYearId);
}
