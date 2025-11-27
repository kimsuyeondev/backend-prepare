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
 * 동시성 처리 없는 기본 주문 서비스
 * 문제점: 동시에 여러 주문이 들어오면 재고가 정확히 차감되지 않음 (Over-selling 발생)
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;

    /**
     * 동시성 처리 없는 주문 생성 - 문제가 있는 코드!
     *
     * @Transactional 제거 → 동시성 문제를 명확하게 재현하기 위함
     * @Transactional 없으면 즉시 커밋되어 락이 풀려버림
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();

        // 각 주문 항목에 대해 재고 차감
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            itemRequest.validate();

            // 메뉴 조회 as-is
/*            Menu menu = menuRepository.findById(itemRequest.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다: " + itemRequest.getMenuId()));*/
            Menu menu = menuRepository.findByIdWithPessimisticLock(itemRequest.getMenuId()).orElseThrow(
                    () -> new IllegalArgumentException("존재하지 않는 메뉴입니다" + itemRequest.getMenuId()));

            // 동시성 문제를 명확히 재현하기 위한 의도적인 지연
            // 실제 프로덕션 코드에서는 네트워크 지연, DB 조회 등으로 자연스럽게 발생
            try {
                Thread.sleep(100); // 재고 조회 후 차감 전에 지연
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 재고 차감 (동시성 문제 발생 지점!)
            menu.decreaseStock(itemRequest.getQuantity());

            // 주문 항목 생성
            OrderItem orderItem = OrderItem.builder()
                    .menuId(menu.getId())
                    .menuName(menu.getName())
                    .quantity(itemRequest.getQuantity())
                    .price(menu.getPrice())
                    .build();

            orderItems.add(orderItem);
        }

        // 주문 생성 및 저장
        Order order = Order.builder()
                .userId(request.getUserId())
                .items(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
