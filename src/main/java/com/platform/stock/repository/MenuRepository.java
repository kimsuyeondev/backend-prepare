package com.platform.stock.repository;

import com.platform.stock.domain.Menu;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * Pessimistic Lock (비관적 락) - 읽을 때부터 락 획득
     * 동시성이 높은 환경에서 안전하지만 성능 저하 가능
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Menu m WHERE m.id = :id")
    Optional<Menu> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * Optimistic Lock (낙관적 락) - @Version 사용
     * 충돌이 적은 환경에서 성능이 좋음
     * Menu 엔티티의 @Version 필드와 함께 사용
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT m FROM Menu m WHERE m.id = :id")
    Optional<Menu> findByIdWithOptimisticLock(@Param("id") Long id);

}
