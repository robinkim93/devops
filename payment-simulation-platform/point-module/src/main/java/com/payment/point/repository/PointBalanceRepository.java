package com.payment.point.repository;

import com.payment.point.entity.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(Long userId);
}
