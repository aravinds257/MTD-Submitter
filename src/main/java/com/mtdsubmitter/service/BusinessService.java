package com.mtdsubmitter.service;

import com.mtdsubmitter.model.Business;
import com.mtdsubmitter.model.TaxYear;
import com.mtdsubmitter.model.enums.BusinessType;
import com.mtdsubmitter.repository.BusinessRepository;
import com.mtdsubmitter.repository.TaxYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing businesses (self-employment and property income sources).
 */
@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final TaxYearRepository taxYearRepository;
    private final AuditService auditService;

    public BusinessService(BusinessRepository businessRepository,
                           TaxYearRepository taxYearRepository,
                           AuditService auditService) {
        this.businessRepository = businessRepository;
        this.taxYearRepository = taxYearRepository;
        this.auditService = auditService;
    }

    public List<Business> getActiveBusinesses(UUID userId) {
        return businessRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Optional<Business> getBusiness(UUID businessId, UUID userId) {
        return businessRepository.findByIdAndUserId(businessId, userId);
    }

    @Transactional
    public Business createBusiness(UUID userId, String tradingName, BusinessType businessType,
                                   String accountingType, String description) {
        Business business = Business.builder()
                .userId(userId)
                .tradingName(tradingName)
                .businessType(businessType)
                .accountingType(com.mtdsubmitter.model.enums.AccountingType.valueOf(accountingType))
                .description(description)
                .isActive(true)
                .build();

        business = businessRepository.save(business);
        auditService.log(userId, "Business", business.getId(), "CREATE", null, null);
        return business;
    }

    @Transactional
    public Business updateBusiness(UUID businessId, UUID userId, String tradingName,
                                   String description) {
        Business business = businessRepository.findByIdAndUserId(businessId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found"));

        business.setTradingName(tradingName);
        business.setDescription(description);
        business = businessRepository.save(business);
        auditService.log(userId, "Business", businessId, "UPDATE", null, null);
        return business;
    }

    @Transactional
    public void deactivateBusiness(UUID businessId, UUID userId) {
        Business business = businessRepository.findByIdAndUserId(businessId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found"));

        business.setActive(false);
        businessRepository.save(business);
        auditService.log(userId, "Business", businessId, "DELETE", null, null);
    }

    /**
     * Get the current tax year, or throw if we're outside a configured tax year.
     */
    public TaxYear getCurrentTaxYear() {
        return taxYearRepository.findCurrent()
                .orElseThrow(() -> new RuntimeException("No tax year configured for the current date"));
    }
}
