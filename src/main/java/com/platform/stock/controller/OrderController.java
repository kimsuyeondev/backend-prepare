package com.platform.stock.controller;

import com.platform.stock.dto.CreateOrderRequest;
import com.platform.stock.dto.OrderResponse;
import com.platform.stock.service.OrderService;
import com.platform.stock.service.OrderServiceWithOptimisticLock;
import com.platform.stock.service.OrderServiceWithPessimisticLock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문 API (동시성 처리)", description = "재고 관리 동시성 처리 문제")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderServiceWithPessimisticLock pessimisticLockService;
    private final OrderServiceWithOptimisticLock optimisticLockService;

    @Operation(summary = "주문 생성 (동시성 처리 없음)", description = "문제가 있는 코드 - 동시 주문시 재고 오류 발생 가능")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @Operation(summary = "주문 생성 (Pessimistic Lock)", description = "비관적 락 사용 - 안전하지만 성능 저하 가능")
    @PostMapping("/pessimistic")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrderWithPessimisticLock(@Valid @RequestBody CreateOrderRequest request) {
        return pessimisticLockService.createOrder(request);
    }

    @Operation(summary = "주문 생성 (Optimistic Lock)", description = "낙관적 락 사용 - 성능 좋지만 충돌시 재시도")
    @PostMapping("/optimistic")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrderWithOptimisticLock(@Valid @RequestBody CreateOrderRequest request) {
        return optimisticLockService.createOrder(request);
    }

    @Operation(summary = "주문 상세 조회")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @Operation(summary = "사용자별 주문 목록 조회")
    @GetMapping
    public List<OrderResponse> getOrdersByUserId(@RequestParam Long userId) {
        return orderService.getOrdersByUserId(userId);
    }
}
