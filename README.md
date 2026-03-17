# Order Service

주문 생성, 조회, 취소를 담당하는 서비스입니다. 주문 생성 시 주문을 `PENDING` 상태로 저장하고 `order.created` 이벤트를 발행합니다. 이후 재고 서비스의 처리 결과를 받아 주문 상태를 `CONFIRMED` 또는 `CANCELLED`로 변경합니다.

## 기술 정보

- Java: `21`
- Spring Boot: `3.4.3`
- Database: `H2 In-Memory`
- Messaging: `Kafka`
- Port: `8081`

## API 목록

- `POST /orders`
  - 주문 생성
- `GET /orders/{orderId}`
  - 주문 조회
- `DELETE /orders/{orderId}`
  - 주문 취소

## 이벤트 목록

- 발행
  - `order.created`
  - `order.cancelled`
- 구독
  - `inventory.deducted`
  - `inventory.insufficient`

## 프로젝트 구조

- `src/main/java/com/toy/order/application`
  - 주문 유스케이스 처리
- `src/main/java/com/toy/order/domain`
  - 주문 도메인 모델과 저장소 인터페이스
- `src/main/java/com/toy/order/infrastructure`
  - Kafka 설정, 이벤트 처리, JPA 기반 영속성 구현
- `src/main/java/com/toy/order/presentation`
  - REST API Controller와 DTO
- `src/main/resources/application.yml`
  - 실행 포트, H2, Kafka 설정

## 실행 메모

- 주문 생성 직후 응답 상태는 보통 `PENDING`입니다.
- 최종 상태는 재고 이벤트 처리 후 `CONFIRMED` 또는 `CANCELLED`로 변경됩니다.
