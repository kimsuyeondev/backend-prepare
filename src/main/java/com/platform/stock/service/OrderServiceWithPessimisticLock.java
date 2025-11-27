package com.platform.stock.service;

import com.platform.stock.domain.Menu;
import com.platform.stock.domain.Order;
import com.platform.stock.domain.OrderItem;
import com.platform.stock.dto.CreateOrderRequest;
import com.platform.stock.dto.OrderResponse;
import com.platform.stock.repository.MenuRepository;
import com.platform.stock.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pessimistic Lock (비관적 락)을 사용한 동시성 처리
 *
 * 장점:
 * - 데이터 정합성 보장 (충돌 발생 X)
 * - 구현이 간단
 *
 * 단점:
 * - 성능 저하 (대기 시간 발생)
 * - 데드락 위험
 * - 동시성이 높은 환경에서 병목 발생
 *
 * 사용 시나리오:
 * - 충돌이 자주 발생하는 경우
 * - 재고 관리처럼 정확성이 중요한 경우
 */
@Service
@RequiredArgsConstructor
public class OrderServiceWithPessimisticLock {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            itemRequest.validate();

            // Pessimistic Lock으로 메뉴 조회 (SELECT ... FOR UPDATE)
            // 다른 트랜잭션은 이 row를 읽거나 수정할 수 없음 (대기)
            Menu menu = menuRepository.findByIdWithPessimisticLock(itemRequest.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다: " + itemRequest.getMenuId()));

            // 재고 차감 (Lock이 걸려있어 안전)
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

        return OrderResponse.from(savedOrder);
    }
}
