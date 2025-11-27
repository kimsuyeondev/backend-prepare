package com.platform.stock.service;

import com.platform.stock.domain.Menu;
import com.platform.stock.domain.Order;
import com.platform.stock.domain.OrderItem;
import com.platform.stock.dto.CreateOrderRequest;
import com.platform.stock.dto.OrderResponse;
import com.platform.stock.repository.MenuRepository;
import com.platform.stock.repository.OrderRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimistic Lock (낙관적 락)을 사용한 동시성 처리
 *
 * 장점:
 * - 성능이 좋음 (Lock 대기 없음)
 * - 동시성이 높은 환경에서 유리
 *
 * 단점:
 * - 충돌 발생시 재시도 필요
 * - 재시도 로직 구현 필요
 * - 충돌이 잦으면 오히려 비효율적
 *
 * 사용 시나리오:
 * - 충돌이 드문 경우
 * - 읽기가 많고 쓰기가 적은 경우
 * - 높은 동시성이 필요한 경우
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceWithOptimisticLock {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;

    private static final int MAX_RETRY = 3;

    /**
     * Optimistic Lock with Retry
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            try {
                return attemptCreateOrder(request);
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("Optimistic Lock 충돌 발생. 재시도 {}/{}", retryCount, MAX_RETRY);

                if (retryCount >= MAX_RETRY) {
                    throw new IllegalStateException("주문 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
                }

                // 짧은 대기 후 재시도
                try {
                    Thread.sleep(50 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("주문 처리가 중단되었습니다.", ie);
                }
            }
        }

        throw new IllegalStateException("주문 처리에 실패했습니다.");
    }

    private OrderResponse attemptCreateOrder(CreateOrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            itemRequest.validate();

            // Optimistic Lock으로 메뉴 조회
            // @Version 필드를 이용해 충돌 감지
            Menu menu = menuRepository.findByIdWithOptimisticLock(itemRequest.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다: " + itemRequest.getMenuId()));

            // 재고 차감
            menu.decreaseStock(itemRequest.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                .menuId(menu.getId())
                .menuName(menu.getName())
                .quantity(itemRequest.getQuantity())
                .price(menu.getPrice())
                .build();

            orderItems.add(orderItem);
        }

        Order order = Order.builder()
            .userId(request.getUserId())
            .items(orderItems)
            .build();

        Order savedOrder = orderRepository.save(order);

        // 트랜잭션 커밋 시점에 @Version 체크
        // 다른 트랜잭션이 먼저 수정했으면 OptimisticLockException 발생

        return OrderResponse.from(savedOrder);
    }
}
