-- 메뉴 초기 데이터
INSERT INTO menu (name, price, stock, version) VALUES ('불고기버거', 8000, 50, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('치즈버거', 7000, 30, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('새우버거', 9000, 20, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('감자튀김', 3000, 100, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('콜라', 2000, 200, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('치킨세트', 18000, 10, 0);
INSERT INTO menu (name, price, stock, version) VALUES ('피자 (라지)', 25000, 15, 0);

-- 쿠폰 초기 데이터

-- 정액 쿠폰 (3000원 할인, 최소 주문 15000원)
INSERT INTO coupon (name, type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, total_quantity, issued_quantity)
VALUES ('신규 가입 쿠폰', 'FIXED', 3000, 15000, NULL, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 1000, 0);

-- 정액 쿠폰 (5000원 할인, 최소 주문 30000원)
INSERT INTO coupon (name, type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, total_quantity, issued_quantity)
VALUES ('배달비 무료 쿠폰', 'FIXED', 5000, 30000, NULL, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 500, 0);

-- 정률 쿠폰 (10% 할인, 최대 5000원, 최소 주문 20000원)
INSERT INTO coupon (name, type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, total_quantity, issued_quantity)
VALUES ('단골 고객 10% 할인', 'PERCENTAGE', 10, 20000, 5000, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 100, 0);

-- 정률 쿠폰 (20% 할인, 최대 10000원, 최소 주문 50000원)
INSERT INTO coupon (name, type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, total_quantity, issued_quantity)
VALUES ('VIP 20% 할인', 'PERCENTAGE', 20, 50000, 10000, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 50, 0);

-- 선착순 100명 쿠폰 (동시성 테스트용)
INSERT INTO coupon (name, type, discount_value, min_order_amount, max_discount_amount, start_date, end_date, total_quantity, issued_quantity)
VALUES ('선착순 100명 특별 쿠폰', 'FIXED', 10000, 10000, NULL, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 100, 0);
