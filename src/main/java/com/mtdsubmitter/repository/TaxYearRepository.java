package com.mtdsubmitter.repository;

import com.mtdsubmitter.model.TaxYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TaxYearRepository extends JpaRepository<TaxYear, Integer> {
    Optional<TaxYear> findByLabel(String label);

    @Query("SELECT ty FROM TaxYear ty WHERE ty.startDate <= :date AND ty.endDate >= :date")
    Optional<TaxYear> findByDate(@Param("date") LocalDate date);

    default Optional<TaxYear> findCurrent() {
        return findByDate(LocalDate.now());
    }
}
