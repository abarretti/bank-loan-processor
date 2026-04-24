package com.ab.bankloanprocessor.repository;

import com.ab.bankloanprocessor.entity.Collateral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CollateralRepository extends JpaRepository<Collateral, UUID> {
}
