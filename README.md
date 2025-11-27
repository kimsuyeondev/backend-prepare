# Backend Prepare - 
CLAUDE CODE AI로 전적으로 작업하면서 비관적락, 낙관적 락을 JAVA, JPA로 학습한다.

실제 플랫폼에서 나올 법한 **실무 중심 과제**를 연습할 수 있는 프로젝트입니다.

---

## 🚀 빠른 시작

### 1. 프로젝트 실행

```bash
cd backend-prepare
./gradlew bootRun
```

### 2. API 문서 확인

브라우저에서 접속:
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (비워두기)

---

## 📚 포함된 문제

### ✅ 1. 동시성 처리 (재고 관리) - 완성!

**문제**: 여러 사용자가 동시에 주문할 때 재고가 정확하게 차감되도록 구현

**구현된 3가지 방법**:
1. **동시성 처리 없음** (`OrderService`) - 문제가 있는 코드
2. **Pessimistic Lock** (`OrderServiceWithPessimisticLock`) - 비관적 락
3. **Optimistic Lock** (`OrderServiceWithOptimisticLock`) - 낙관적 락

#### 테스트 방법

```bash
# 동시성 테스트 실행
./gradlew test --tests OrderConcurrencyTest

# 출력 결과 예시:
# === Pessimistic Lock 사용 ===
# 성공한 주문 수: 10
# 실패한 주문 수: 90
# 최종 재고: 0
```

#### API 호출 예시

**1) 메뉴 조회**
```bash
curl http://localhost:8080/api/menus
```

**2) 주문 생성 (동시성 처리 없음)**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "items": [
      {
        "menuId": 1,
        "quantity": 2
      }
    ]
  }'
```

**3) 주문 생성 (Pessimistic Lock)**
```bash
curl -X POST http://localhost:8080/api/orders/pessimistic \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "items": [
      {
        "menuId": 1,
        "quantity": 2
      }
    ]
  }'
```

**4) 주문 생성 (Optimistic Lock)**
```bash
curl -X POST http://localhost:8080/api/orders/optimistic \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "items": [
      {
        "menuId": 1,
        "quantity": 2
      }
    ]
  }'
```

---

## 🧪 동시성 테스트 시나리오

### 시나리오
- 재고 10개인 메뉴
- 100명이 동시에 각 1개씩 주문
- 예상: 10개만 주문 성공, 90개 실패, 최종 재고 0

### 테스트 결과

**중요:** H2 in-memory DB는 기본적으로 어느 정도 동시성을 처리하기 때문에, 테스트 환경에서는 문제가 재현되지 않을 수 있습니다.

하지만 **실제 프로덕션 환경**에서는:
- 네트워크 지연
- 복잡한 비즈니스 로직
- 높은 트래픽
- 분산 서버 환경

이런 요인들로 인해 동시성 문제가 발생할 확률이 매우 높습니다!

### 방법별 비교

| 방법 | Over-selling | 성능 | 복잡도 | 프로덕션 안정성 |
|------|-------------|------|--------|----------------|
| 동시성 처리 없음 | ❌ 발생 가능 | 빠름 | 단순 | ❌ 위험 |
| Pessimistic Lock | ✅ 방지 | 느림 | 단순 | ✅ 안전 |
| Optimistic Lock | ✅ 방지 | 빠름 | 복잡 (재시도 필요) | ✅ 안전 |

---

## 📖 학습 포인트

### 1. Pessimistic Lock (비관적 락)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Menu> findByIdWithPessimisticLock(Long id);
```
- **동작**: `SELECT ... FOR UPDATE` 쿼리 실행
- **장점**: 데이터 정합성 완벽 보장
- **단점**: 대기 시간 발생, 성능 저하
- **사용처**: 충돌이 자주 발생하는 경우 (재고, 좌석 예약)

### 2. Optimistic Lock (낙관적 락)
```java
@Version
private Long version;
```
- **동작**: Version 필드로 충돌 감지
- **장점**: Lock 대기 없음, 성능 좋음
- **단점**: 충돌시 재시도 필요
- **사용처**: 충돌이 적은 경우 (게시글 수정 등)

### 3. 재시도 로직 (Optimistic Lock)
```java
int retryCount = 0;
while (retryCount < MAX_RETRY) {
    try {
        return attemptCreateOrder(request);
    } catch (OptimisticLockException e) {
        retryCount++;
        Thread.sleep(50 * retryCount); // Exponential backoff
    }
}
```

---

## 🗂️ 프로젝트 구조

```
backend-prepare/
├── src/
│   ├── main/
│   │   ├── java/com/platform/
│   │   │   ├── BackendPrepareApplication.java
│   │   │   └── stock/                    # 동시성 처리 문제
│   │   │       ├── controller/
│   │   │       │   ├── OrderController.java
│   │   │       │   └── MenuController.java
│   │   │       ├── service/
│   │   │       │   ├── OrderService.java                        # 동시성 X
│   │   │       │   ├── OrderServiceWithPessimisticLock.java    # 비관적 락
│   │   │       │   ├── OrderServiceWithOptimisticLock.java     # 낙관적 락
│   │   │       │   └── MenuService.java
│   │   │       ├── repository/
│   │   │       │   ├── MenuRepository.java
│   │   │       │   └── OrderRepository.java
│   │   │       ├── domain/
│   │   │       │   ├── Menu.java
│   │   │       │   ├── Order.java
│   │   │       │   ├── OrderItem.java
│   │   │       │   └── OrderStatus.java
│   │   │       └── dto/
│   │   │           ├── CreateOrderRequest.java
│   │   │           ├── OrderResponse.java
│   │   │           └── MenuResponse.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data.sql               # 초기 데이터
│   └── test/
│       └── java/com/platform/stock/
│           └── service/
│               └── OrderConcurrencyTest.java  # 동시성 테스트
├── build.gradle
└── README.md
```

---

## 💡 연습 방법

### Step 1: 코드 읽기
1. `OrderService` (동시성 처리 없음) 먼저 읽기
2. 어떤 문제가 있는지 생각해보기
3. `OrderServiceWithPessimisticLock`과 비교
4. `OrderServiceWithOptimisticLock`의 재시도 로직 이해

### Step 2: 테스트 실행
```bash
./gradlew test --tests OrderConcurrencyTest
```
- 각 방법의 결과 차이 확인
- 콘솔 로그로 SQL 쿼리 확인

### Step 3: 직접 수정해보기
- 재시도 횟수 조정 (MAX_RETRY)
- Exponential backoff 시간 변경
- 초기 재고 수량 변경
- 동시 요청 수 변경

### Step 4: 실전 연습
Swagger UI에서 직접 API 호출해보기:
1. 여러 탭에서 동시에 주문 요청
2. H2 Console에서 실시간 재고 확인
3. 각 방법의 응답 시간 비교

---

## 

### Q1. Pessimistic Lock과 Optimistic Lock의 차이는?
**답변**:
- Pessimistic Lock은 데이터를 읽을 때부터 락을 걸어 다른 트랜잭션의 접근을 차단합니다. 충돌이 자주 발생하는 환경에서 안전하지만 대기 시간이 발생합니다.
- Optimistic Lock은 락을 걸지 않고 Version 필드로 충돌을 감지합니다. 성능은 좋지만 충돌 시 재시도가 필요합니다.

### Q2. 재고가 10개인데 100명이 동시 주문하면?
**답변**:
- 동시성 처리가 없으면 Over-selling이 발생할 수 있습니다.
- Pessimistic Lock을 사용하면 먼저 온 10명만 성공하고 나머지는 대기 후 실패합니다.
- Optimistic Lock을 사용하면 충돌 발생 시 재시도하며, 최종적으로 10명만 성공합니다.

### Q3. 실제 서비스에서는 어떤 방법을 사용하나요?
**답변**:
- 재고 관리는 Pessimistic Lock이나 Redis 분산 락 사용
- 게시글 수정 등은 Optimistic Lock 사용
- 트래픽이 매우 높으면 Redis + 메시지 큐 조합

---

## 🔜 다음 문제 (Coming Soon)

- [ ] 쿠폰 할인 시스템 (비즈니스 로직 설계)
- [ ] 배달매칭 알고리즘 (거리 기반 최적화)
- [ ] API 설계 및 구현
- [ ] 레거시 코드 리팩토링

---

## 🛠️ 기술 스택

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (In-memory)
- Lombok
- JUnit 5
- SpringDoc OpenAPI (Swagger)
