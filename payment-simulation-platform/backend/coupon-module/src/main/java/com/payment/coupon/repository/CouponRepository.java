package com.payment.coupon.repository;

import com.payment.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByUserId(Long userId);
    List<Coupon> findByUserIdAndStatus(Long userId, Coupon.CouponStatus status);
}
