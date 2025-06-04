package com.cinefinder.theater.data.repository;

import com.cinefinder.theater.data.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    @Query("SELECT t FROM Theater t JOIN FETCH t.brand WHERE t.brand.name = :brandName")
    List<Theater> findByBrandName(String brandName);
    Optional<Theater> findByBrandNameAndCode(String brandName, String code);
}
