package com.platform.stock.service;

import com.platform.stock.domain.Menu;
import com.platform.stock.dto.CreateOrderRequest;
import com.platform.stock.repository.MenuRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 테스트
 *
 * 시나리오:
 * - 재고가 10개인 메뉴에 대해
 * - 100명의 사용자가 동시에
 * - 각각 1개씩 주문
 *
 * 예상 결과:
 * - 10개만 주문 성공
 * - 최종 재고는 0
 * - Over-selling 발생하지 않음
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderServiceWithPessimisticLock pessimisticLockService;

    @Autowired
    private OrderServiceWithOptimisticLock optimisticLockService;

    @Autowired
    private MenuRepository menuRepository;

    private Long testMenuId;
    private static final int INITIAL_STOCK = 10;
    private static final int THREAD_COUNT = 100;

    @BeforeEach
    void setUp() {
        // 테스트용 메뉴 생성
        Menu testMenu = Menu.builder()
            .name("테스트 치킨")
            .price(18000)
            .stock(INITIAL_STOCK)
            .build();
        testMenuId = menuRepository.save(testMenu).getId();
    }

    @AfterEach
    void tearDown() {
        menuRepository.deleteById(testMenuId);
    }

    @Test
    @DisplayName("동시성 처리 없는 경우 - 문제 시연용 (검증 없음)")
    void testWithoutConcurrencyControl() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 주문
        for (int i = 0; i < THREAD_COUNT; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(userId)
                        .items(List.of(
                            CreateOrderRequest.OrderItemRequest.builder()
                                .menuId(testMenuId)
                                .quantity(1)
                                .build()
                        ))
                        .build();

                    orderService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 재고 확인
        Menu menu = menuRepository.findById(testMenuId).orElseThrow();
        int finalStock = menu.getStock();
        int totalOrders = successCount.get();

        System.out.println("\n=== 동시성 처리 없는 경우 ===");
        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failCount.get());
        System.out.println("최종 재고: " + finalStock);
        System.out.println("예상 재고: 0");
        System.out.println("실제 판매량: " + (INITIAL_STOCK - finalStock));
        System.out.println("Over-selling 발생: " + (finalStock < 0 ? "YES" : "NO"));

        // 문제점 분석
        if (totalOrders > INITIAL_STOCK && finalStock >= 0) {
            System.out.println("⚠️ 동시성 문제 발생!");
            System.out.println("   -> 재고 10개인데 " + totalOrders + "개가 주문 성공");
            System.out.println("   -> 재고 차감 로직에서 Race Condition 발생");
            throw new RuntimeException("   -> 재고 10개인데 " + totalOrders + "개가 주문 성공");

        } else if (finalStock < 0) {
            System.out.println("⚠️ Over-selling 발생!");
            System.out.println("   -> 재고가 음수가 됨: " + finalStock);
            throw new RuntimeException("   -> 재고가 음수가 됨: " + finalStock);
        } else if (totalOrders == INITIAL_STOCK && finalStock == 0) {
            System.out.println("✅ 이번에는 운 좋게 정상 작동 (하지만 보장 안됨)");
            System.out.println("   -> DB의 기본 격리 수준이나 타이밍 덕분");
            System.out.println("   -> 프로덕션에서는 문제 발생 가능성 높음");

        }
        System.out.println();

        // 이 테스트는 문제를 보여주기 위한 것이므로 assert 없음
        // 환경에 따라 결과가 달라질 수 있음
    }

    @Test
    @DisplayName("Pessimistic Lock 사용 - 정확히 10개만 판매 (성공)")
    void testWithPessimisticLock() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 주문
        for (int i = 0; i < THREAD_COUNT; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(userId)
                        .items(List.of(
                            CreateOrderRequest.OrderItemRequest.builder()
                                .menuId(testMenuId)
                                .quantity(1)
                                .build()
                        ))
                        .build();

                    pessimisticLockService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Menu menu = menuRepository.findById(testMenuId).orElseThrow();

        System.out.println("=== Pessimistic Lock 사용 ===");
        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failCount.get());
        System.out.println("최종 재고: " + menu.getStock());

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 10개만 성공
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - INITIAL_STOCK); // 90개 실패
        assertThat(menu.getStock()).isEqualTo(0); // 재고 0
    }

    @Test
    @DisplayName("Optimistic Lock 사용 - 정확히 10개만 판매 (성공)")
    void testWithOptimisticLock() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 주문
        for (int i = 0; i < THREAD_COUNT; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    CreateOrderRequest request = CreateOrderRequest.builder()
                        .userId(userId)
                        .items(List.of(
                            CreateOrderRequest.OrderItemRequest.builder()
                                .menuId(testMenuId)
                                .quantity(1)
                                .build()
                        ))
                        .build();

                    optimisticLockService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Menu menu = menuRepository.findById(testMenuId).orElseThrow();

        System.out.println("=== Optimistic Lock 사용 ===");
        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failCount.get());
        System.out.println("최종 재고: " + menu.getStock());

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 10개만 성공
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - INITIAL_STOCK); // 90개 실패
        assertThat(menu.getStock()).isEqualTo(0); // 재고 0
    }
}
