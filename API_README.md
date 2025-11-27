# 🎫 API 구현 문제 - 쿠폰 할인 시스템

> ** 실무 스타일 과제**: API 구현 + 복잡한 비즈니스 로직 설계

---

## 📋 문제 설명

플랫폼의 **쿠폰 할인 시스템**을 구현하세요.

### 핵심 요구사항

1. **쿠폰 발급 API** - 선착순 쿠폰 발급 (동시성 처리 필요)
2. **할인 금액 계산 API** - 여러 쿠폰 조합 시 할인 금액 계산

---

## 🎯 구현해야 할 기능

### 1️⃣ 쿠폰 발급 API

**엔드포인트**: `POST /api/coupons/issue`

**요청**:
```json
{
  "userId": 123,
  "couponId": 1
}
```

**응답**:
```json
{
  "id": 1,
  "name": "신규 가입 쿠폰",
  "type": "FIXED",
  "discountValue": 3000,
  "minOrderAmount": 15000,
  "maxDiscountAmount": null,
  "startDate": "2025-01-01T00:00:00",
  "endDate": "2025-12-31T23:59:59",
  "totalQuantity": 1000,
  "issuedQuantity": 1,
  "remainingQuantity": 999
}
```

#### 비즈니스 로직

- ✅ 쿠폰 발급 가능 여부 확인 (기간, 수량)
- ✅ **동시성 처리** (100명이 동시에 발급 요청해도 정확히 100개만 발급)
- ✅ 사용자당 동일 쿠폰 중복 발급 방지
- ✅ 발급 후 `issuedQuantity` 증가

#### 예외 상황

| 상황 | HTTP Status | 에러 메시지 |
|------|-------------|------------|
| 쿠폰이 존재하지 않음 | 400 | "쿠폰이 존재하지 않습니다" |
| 발급 기간이 아님 | 400 | "쿠폰 발급이 불가능합니다" |
| 쿠폰 소진 | 400 | "쿠폰 발급이 불가능합니다" |
| 이미 발급받음 | 400 | "이미 발급받은 쿠폰입니다" |

---

### 2️⃣ 할인 금액 계산 API

**엔드포인트**: `POST /api/coupons/calculate`

**요청**:
```json
{
  "userId": 123,
  "orderAmount": 50000,
  "couponIds": [1, 3]
}
```

**응답**:
```json
{
  "originalAmount": 50000,
  "totalDiscount": 8000,
  "finalAmount": 42000,
  "appliedCoupons": [
    {
      "couponId": 3,
      "couponName": "단골 고객 10% 할인",
      "discountAmount": 5000
    },
    {
      "couponId": 1,
      "couponName": "신규 가입 쿠폰",
      "discountAmount": 3000
    }
  ]
}
```

#### 복잡한 비즈니스 로직

1. **쿠폰 보유 여부 확인**
   - 사용자가 실제로 보유한 쿠폰인지 확인
   - 이미 사용한 쿠폰은 제외

2. **쿠폰 유효성 검증**
   - 쿠폰 유효기간 확인
   - 최소 주문 금액 조건 확인

3. **할인 금액 계산 (핵심!)**
   ```
   [정액 쿠폰]
   - 할인 금액 = discountValue
   - 예: 3000원 할인 쿠폰 → 3000원 할인

   [정률 쿠폰]
   - 할인 금액 = 주문금액 × (discountValue / 100)
   - 최대 할인 금액 제한 적용
   - 예: 10% 할인 (최대 5000원)
     - 주문 30000원 → 3000원 할인
     - 주문 60000원 → 6000원이지만 최대 5000원만 할인
   ```

4. **쿠폰 적용 순서 최적화**
   - 할인율이 높은 쿠폰부터 적용
   - 정률 쿠폰 → 정액 쿠폰 순서로 적용

5. **최종 금액 검증**
   - 총 할인 금액이 주문 금액을 초과할 수 없음
   - 최종 결제 금액은 0원 이상

#### 예외 상황

| 상황 | HTTP Status | 에러 메시지 |
|------|-------------|------------|
| 쿠폰을 보유하지 않음 | 400 | "보유하지 않은 쿠폰입니다" |
| 이미 사용한 쿠폰 | 400 | "이미 사용한 쿠폰입니다" |
| 쿠폰 유효기간 아님 | 400 | "쿠폰 사용 기간이 아닙니다" |
| 최소 주문 금액 미달 | 400 | "최소 주문 금액을 만족하지 않습니다" |

---

## 💡 구현 힌트

### 1. 동시성 처리 (쿠폰 발급)

```java
@Transactional
public CouponResponse issueCoupon(IssueCouponRequest request) {
    // 1. Pessimistic Lock으로 쿠폰 조회
    Coupon coupon = couponRepository.findByIdWithLock(request.getCouponId())
        .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다"));

    // 2. 발급 가능 여부 확인
    if (!coupon.canIssue()) {
        throw new IllegalStateException("쿠폰 발급이 불가능합니다");
    }

    // 3. 중복 발급 체크
    // TODO: 이미 발급받은 쿠폰인지 확인

    // 4. 쿠폰 발급
    coupon.issue(); // issuedQuantity++

    // 5. UserCoupon 생성
    // TODO: 사용자 쿠폰 저장

    return CouponResponse.from(coupon);
}
```

### 2. 할인 금액 계산

```java
@Transactional(readOnly = true)
public CalculateDiscountResponse calculateDiscount(CalculateDiscountRequest request) {
    List<AppliedCoupon> appliedCoupons = new ArrayList<>();
    int totalDiscount = 0;

    for (Long couponId : request.getCouponIds()) {
        // 1. 사용자가 보유한 쿠폰인지 확인
        UserCoupon userCoupon = userCouponRepository
            .findByUserIdAndCouponIdAndUsed(request.getUserId(), couponId, false)
            .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다"));

        // 2. 쿠폰 정보 조회
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다"));

        // 3. 쿠폰 유효성 검증
        if (!coupon.isValid()) {
            throw new IllegalStateException("쿠폰 사용 기간이 아닙니다");
        }

        // 4. 할인 금액 계산
        int discount = coupon.calculateDiscount(request.getOrderAmount());
        totalDiscount += discount;

        // 5. 적용된 쿠폰 정보 저장
        appliedCoupons.add(AppliedCoupon.builder()
            .couponId(couponId)
            .couponName(coupon.getName())
            .discountAmount(discount)
            .build());
    }

    // 6. 최종 금액 계산 (음수 방지)
    int finalAmount = Math.max(0, request.getOrderAmount() - totalDiscount);

    return CalculateDiscountResponse.builder()
        .originalAmount(request.getOrderAmount())
        .totalDiscount(totalDiscount)
        .finalAmount(finalAmount)
        .appliedCoupons(appliedCoupons)
        .build();
}
```

---

## 🧪 테스트 시나리오

### 시나리오 1: 쿠폰 발급 동시성 테스트

```java
@Test
void 선착순_100명_쿠폰_발급() throws InterruptedException {
    // given: 쿠폰 100개 발행
    Long couponId = 5L; // "선착순 100명 특별 쿠폰"

    // when: 1000명이 동시에 발급 요청
    ExecutorService executor = Executors.newFixedThreadPool(1000);
    CountDownLatch latch = new CountDownLatch(1000);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < 1000; i++) {
        int userId = i;
        executor.submit(() -> {
            try {
                couponService.issueCoupon(new IssueCouponRequest((long) userId, couponId));
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 실패는 무시
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executor.shutdown();

    // then: 정확히 100명만 발급 성공
    assertThat(successCount.get()).isEqualTo(100);

    Coupon coupon = couponRepository.findById(couponId).orElseThrow();
    assertThat(coupon.getIssuedQuantity()).isEqualTo(100);
}
```

### 시나리오 2: 할인 금액 계산

```java
@Test
void 여러_쿠폰_조합_할인_계산() {
    // given
    Long userId = 1L;

    // 쿠폰 발급
    couponService.issueCoupon(new IssueCouponRequest(userId, 1L)); // 3000원 할인
    couponService.issueCoupon(new IssueCouponRequest(userId, 3L)); // 10% 할인 (최대 5000원)

    // when: 50000원 주문에 두 쿠폰 사용
    CalculateDiscountRequest request = new CalculateDiscountRequest(
        userId, 50000, List.of(1L, 3L)
    );
    CalculateDiscountResponse response = couponService.calculateDiscount(request);

    // then
    assertThat(response.getOriginalAmount()).isEqualTo(50000);
    assertThat(response.getTotalDiscount()).isEqualTo(8000); // 5000 + 3000
    assertThat(response.getFinalAmount()).isEqualTo(42000);
    assertThat(response.getAppliedCoupons()).hasSize(2);
}
```

---

## 📂 파일 구조

```
com.api.coupon/
├── controller/
│   └── CouponController.java          # REST API 컨트롤러
├── service/
│   ├── CouponService.java             # ⭐ 여기에 구현하세요!
│   └── CouponServiceAnswer.java       # 정답 코드 (참고용)
├── repository/
│   ├── CouponRepository.java
│   └── UserCouponRepository.java
├── domain/
│   ├── Coupon.java                    # 쿠폰 엔티티
│   ├── CouponType.java                # FIXED / PERCENTAGE
│   └── UserCoupon.java                # 사용자 보유 쿠폰
└── dto/
    ├── IssueCouponRequest.java
    ├── CalculateDiscountRequest.java
    ├── CalculateDiscountResponse.java
    └── CouponResponse.java
```

---

## 🚀 실행 방법

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. 초기 데이터 확인

브라우저에서 H2 Console 접속:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`

```sql
-- 쿠폰 목록 확인
SELECT * FROM coupon;
```

### 3. API 테스트

**쿠폰 발급**:
```bash
curl -X POST http://localhost:8080/api/coupons/issue \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "couponId": 1
  }'
```

**할인 금액 계산**:
```bash
curl -X POST http://localhost:8080/api/coupons/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "orderAmount": 50000,
    "couponIds": [1, 3]
  }'
```

---

## 🎓 면접 포인트

### 1. 동시성 처리
**Q**: 선착순 쿠폰 발급에서 왜 Pessimistic Lock을 사용했나요?

**A**: 쿠폰 발급은 충돌이 자주 발생하는 작업입니다. 100개 한정 쿠폰에 1000명이 몰리면 대부분의 요청이 실패하므로, 낙관적 락보다는 비관적 락으로 순차 처리하는 게 효율적입니다. `SELECT ... FOR UPDATE`로 락을 걸어 정확히 100개만 발급되도록 보장했습니다.

### 2. 비즈니스 로직 설계
**Q**: 여러 쿠폰을 조합할 때 어떤 순서로 적용하나요?

**A**: 할인율이 높은 쿠폰부터 적용하며, 일반적으로 정률 쿠폰을 먼저 적용한 후 정액 쿠폰을 적용합니다. 이렇게 하면 고객에게 최대 할인 혜택을 제공할 수 있습니다. 또한 최종 금액이 음수가 되지 않도록 검증합니다.

### 3. 예외 처리
**Q**: 쿠폰 발급 실패 시 어떻게 처리하나요?

**A**:
- 쿠폰 소진: "쿠폰 발급이 불가능합니다" (400 Bad Request)
- 중복 발급: "이미 발급받은 쿠폰입니다" (400 Bad Request)
- 기간 만료: "쿠폰 발급이 불가능합니다" (400 Bad Request)

사용자에게 명확한 에러 메시지를 제공하여 왜 실패했는지 알 수 있도록 했습니다.

---

## ✅ 완료 체크리스트

- [ ] `CouponService.issueCoupon()` 구현
- [ ] `CouponService.calculateDiscount()` 구현
- [ ] 동시성 테스트 작성 및 통과
- [ ] 할인 금액 계산 테스트 작성 및 통과
- [ ] 예외 처리 테스트 작성 및 통과
- [ ] API 실제 호출 테스트 완료

---

## 💬 추가 개선 아이디어

### 1. 쿠폰 조합 규칙
- 특정 쿠폰은 중복 사용 불가 (예: 첫 주문 쿠폰)
- 카테고리별 쿠폰 (치킨 전용, 피자 전용 등)

### 2. 성능 최적화
- 쿠폰 정보 Redis 캐싱
- 발급 수량 Redis로 관리 (DB 부하 감소)

### 3. 실시간 알림
- 쿠폰 발급 성공 시 푸시 알림
- 쿠폰 만료 임박 알림

---

**화이팅! 🚀**
