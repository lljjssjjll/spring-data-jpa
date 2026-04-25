# Spring Data JPA 학습 가이드

> 예제 엔티티: `Member`, `Team`, `Address`, `Order`, `OrderItem`, `Item`, `Book`, `Album`, `Category`, `CategoryItem`

---

## 1. 영속성 컨텍스트

**핵심 개념**
- 1차 캐시 — 같은 id로 두 번 조회 시 쿼리 1번만 발생
- 동일성 보장 — 같은 트랜잭션 내 동일 id 조회 시 `==` 비교 true
- 변경 감지 (Dirty Checking) — `member.age = 20` 후 별도 `save()` 없이 update 쿼리 발생
- 지연 쓰기 (Write-behind) — flush 시점까지 INSERT/UPDATE 모아서 실행
- 준영속 — `em.detach()`, `em.clear()` 후 변경 감지 동작 안 함

**예제 포인트**
```
- Member 저장 후 같은 트랜잭션에서 재조회 → 쿼리 몇 번?
- Member age 변경 후 save() 없이 커밋 → update 쿼리 발생?
- em.clear() 후 변경 감지 동작 여부
```

---

## 2. 연관관계 매핑

**핵심 개념**
- 단방향 vs 양방향
- 연관관계 주인 (`mappedBy`) — 외래키를 가진 쪽이 주인
- 양방향 편의 메서드 (`Member.changeTeam()`)
- 주인이 아닌 쪽에만 값 세팅 시 DB에 반영 안 됨

**예제 포인트**
```
- Team 생성 → Member에 team 세팅 → DB 외래키 확인
- team.members.add(member)만 했을 때 외래키가 저장되는가?
- changeTeam() 편의 메서드 사용 전/후 비교
- Order → OrderItem cascade: OrderItem 별도 저장 없이 Order만 저장
```

---

## 3. 페치 전략 & N+1 문제

**핵심 개념**
- `LAZY` vs `EAGER` — 기본값과 권장 전략
- N+1 문제 — Member 10명 조회 후 team 접근 → 쿼리 11번
- 해결책 3가지
  1. Fetch Join (`@Query("select m from Member m join fetch m.team")`)
  2. `@EntityGraph(attributePaths = ["team"])`
  3. `@BatchSize` / `default_batch_fetch_size`

**예제 포인트**
```
- Member 여러 명 조회 후 m.team.name 접근 → SQL 로그로 N+1 확인
- Fetch Join으로 해결 후 쿼리 수 비교
- 컬렉션 Fetch Join (Order → OrderItem) 페이징 시 경고 확인
- EntityGraph vs Fetch Join 차이
```

---

## 4. Spring Data JPA Repository

**핵심 개념**
- `JpaRepository` 기본 메서드 (save, findById, findAll, delete 등)
- 쿼리 메서드 — 메서드 이름으로 JPQL 자동 생성
- `@Query` — 직접 JPQL 작성
- `@Modifying` — 벌크 update/delete

**예제 포인트**
```
- findByUsernameAndAgeGreaterThan()
- @Query로 DTO 직접 조회
- @Modifying + @Query로 벌크 age 증가
- 벌크 연산 후 영속성 컨텍스트 초기화 필요성 (clearAutomatically)
```

---

## 5. 페이징

**핵심 개념**
- `Pageable` 파라미터 사용
- `Page<T>` — 전체 count 쿼리 포함
- `Slice<T>` — count 쿼리 없음, "더보기" 방식에 적합
- count 쿼리 최적화 (`countQuery` 분리)

**예제 포인트**
```
- findByAge(age, PageRequest.of(0, 3, Sort.by("username")))
- Page vs Slice 반환값 차이 (totalElements 유무)
- 복잡한 쿼리에서 countQuery 별도 지정
```

---

## 6. Projections

**핵심 개념**
- 인터페이스 기반 Projection — 필요한 컬럼만 선택
- DTO 기반 Projection — `@Query` + `new` 키워드
- 중첩 Projection

**예제 포인트**
```
- MemberDto(id, username, teamName) 조회
- 인터페이스 Projection으로 username만 조회
- 동적 Projection (제네릭 타입 파라미터)
```

---

## 7. 트랜잭션

**핵심 개념**
- `@Transactional` 전파 레벨
  - `REQUIRED` (기본) — 기존 트랜잭션 참여
  - `REQUIRES_NEW` — 항상 새 트랜잭션
  - `NESTED` — 중첩 트랜잭션 (savepoint)
- 격리 수준 (READ_COMMITTED, REPEATABLE_READ 등)
- `readOnly = true` — 스냅샷 미생성, 플러시 생략 → 성능 최적화
- self-invocation 문제 — 같은 클래스 내 메서드 호출 시 트랜잭션 미적용

**예제 포인트**
```
- @Transactional(readOnly = true) 조회 메서드에서 변경 감지 동작 안 하는 것 확인
- REQUIRES_NEW로 내부 메서드 별도 커밋 시나리오
- 같은 클래스에서 @Transactional 메서드 호출 → 프록시 우회 문제 확인
```

---

## 8. 감사(Auditing)

**핵심 개념**
- `@EnableJpaAuditing` (JpaConfig)
- `@CreatedDate`, `@LastModifiedDate`
- `BaseEntity` 상속으로 공통 적용

**예제 포인트**
```
- Member 저장 후 createdAt, updatedAt 자동 세팅 확인
- Member 수정 후 updatedAt만 변경 확인
```

---

## 9. 임베디드 타입 (@Embeddable)

**핵심 개념**
- `Address` — city, street, zipcode를 하나의 값 타입으로 묶음
- 컬럼은 Member 테이블에 같이 존재
- `@AttributeOverride`로 컬럼명 재정의 가능

**예제 포인트**
```
- Member에 Address 저장 → 테이블 컬럼 확인
- Address null vs 각 필드 null 차이
```

---

## 10. 상속 매핑

**핵심 개념**
- `SINGLE_TABLE` (기본) — 한 테이블, dtype 컬럼 구분 → 조회 빠름, null 컬럼 많음
- `JOINED` — 부모/자식 테이블 분리 → 정규화, 조회 시 join 발생
- `TABLE_PER_CLASS` — 각 자식마다 별도 테이블 (잘 안 씀)

**예제 포인트**
```
- Item, Book, Album 저장 후 SINGLE_TABLE 구조 확인 (dtype 컬럼)
- @Inheritance(strategy = JOINED)으로 변경 후 테이블 구조 비교
- 부모 타입으로 조회 → 자식 타입 캐스팅
```

---

## 11. 자기참조 연관관계

**핵심 개념**
- `Category.parent` / `Category.children` — 계층 구조
- 무한 순환 주의

**예제 포인트**
```
- 대분류 → 중분류 → 소분류 Category 생성
- children으로 계층 탐색
```

---

## 12. ManyToMany 대체 패턴

**핵심 개념**
- `@ManyToMany` 직접 사용 지양 — 중간 테이블 커스텀 불가
- `CategoryItem` 중간 엔티티로 대체

**예제 포인트**
```
- Book에 Category 2개 연결 (CategoryItem 2개 생성)
- CategoryItem에 추가 컬럼 필요 시 확장 용이성 확인
```

---

## 학습 순서 추천

```
1단계: 영속성 컨텍스트 → 연관관계 → 페치 전략/N+1
2단계: Repository → 페이징 → Projections
3단계: 트랜잭션 → Auditing → 임베디드
4단계: 상속 매핑 → 자기참조 → ManyToMany 대체
```
