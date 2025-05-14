package com.cinefinder.theater.data.repository;

import com.cinefinder.theater.data.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findByBrandName(String brandName);
    void deleteByBrandName(String brandName);
    Theater findByBrandNameAndCode(String brandName, String code);
}
