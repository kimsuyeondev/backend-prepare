package com.api.coupon.repository;

import com.api.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 쿠폰 발급 시 동시성 제어를 위한 Pessimistic Lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon  c where c.id = :id")
    Optional<Coupon> findByIdWithPessmisticLock(@Param("id") Long id);
}
