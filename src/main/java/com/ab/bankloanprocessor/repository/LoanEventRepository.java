package com.ab.bankloanprocessor.repository;

import com.ab.bankloanprocessor.entity.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, UUID> {
}
