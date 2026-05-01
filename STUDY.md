# Spring Data JPA 학습 가이드

> 예제 엔티티: `Member`, `Team`, `Address`, `Order`, `OrderItem`, `Item`, `Book`, `Album`, `Category`, `CategoryItem`

---

## 1. 영속성 컨텍스트

**핵심 개념**
- 1차 캐시 — 같은 id로 두 번 조회 시 쿼리 1번만 발생
- 동일성 vs 동등성 — 같은 트랜잭션 내 동일 id 조회 시 동일한 객체 참조 보장
- 변경 감지 (Dirty Checking) — `member.age = 20` 후 별도 `save()` 없이 update 쿼리 발생
- 지연 쓰기 (Write-behind) — flush 시점까지 INSERT/UPDATE 모아서 실행
- 준영속 — `em.detach()`, `em.clear()` 후 변경 감지 동작 안 함

**1차 캐시**
- `em.persist()` 또는 `em.find()` 시점에 1차 캐시 + 스냅샷 동시 생성
- `em.clear()` 없이 같은 id 재조회 → SELECT 없이 1차 캐시에서 반환
- `em.clear()` 후 재조회 → SELECT 발생 후 새 객체 생성
- IDENTITY 전략: `em.persist()` 시점에 즉시 INSERT → id 반환 → 1차 캐시 저장

**동일성 vs 동등성**
- 동일성 (Identity) `===` — 같은 객체 참조인지 (주소값 비교)
- 동등성 (Equality) `==` — 값이 같은지 (`equals()` 호출)
- JPA는 같은 트랜잭션 내에서 동일 id 조회 시 **동일성** 보장 (`===` true)
- `em.clear()` 후 재조회 → 다른 객체 (`===` false)
- Entity에 `equals()` 미정의 시 `==`도 `===`와 동일하게 동작 (Any 기본 구현)
- Entity는 data class 사용 지양
  - 이유: 지연 로딩 필드를 `hashCode`/`equals`가 건드릴 수 있고, `copy()`가 id를 복사해 비영속 상태처럼 동작할 수 있음
- 실무에서는 엔티티 직접 비교보다 `entity.id == other.id` 방식 사용

**변경 감지 (Dirty Checking)**
- 스냅샷: 영속 상태가 된 시점의 복사본 (persist/find/JPQL 모두 생성)
- flush 시점에 1차 캐시 vs 스냅샷 비교 → 변경된 필드만 UPDATE
- flush 자동 발생 시점 (FlushMode.AUTO)
  1. 트랜잭션 커밋 직전
  2. JPQL 실행 직전 (dirty 엔티티와 같은 테이블 조회 시)
- `@Transactional` 테스트는 롤백으로 끝나므로 커밋이 없어 flush 생략 → UPDATE 미발생
  - 확인하려면 `em.flush()` 명시 또는 `@Commit` 사용

**삭제 (remove)**
- `em.remove(entity)` — 영속 상태 엔티티를 삭제 대상으로 표시
- flush 시점에 DELETE 쿼리 발생 (지연 쓰기 동작)
- 준영속 엔티티는 remove 불가 → `merge()` 후 remove 해야 함

**준영속 → 영속 (merge)**
- `em.merge(entity)` — 준영속 엔티티를 영속 상태로 전환
- 동작: id로 DB SELECT → 준영속 객체의 값을 영속 엔티티에 복사 → 영속 엔티티 반환
- 원본 객체는 여전히 준영속, 반드시 반환값을 사용해야 함
- `em.contains(원본)` = false, `em.contains(merged)` = true

**예제 포인트**
```
- em.clear() 유무에 따른 SELECT 쿼리 횟수 비교
- 동일 id 두 번 조회 후 === 비교
- em.clear() 전후 === 비교
- age 변경 후 @Commit으로 UPDATE 확인
- JPQL 실행 전 자동 flush로 UPDATE 선행 확인
- em.remove() 후 flush → DELETE 쿼리 확인
```

---

## 2. 연관관계 매핑

**핵심 개념**
- 단방향 vs 양방향 — 테이블 구조는 동일, 객체 탐색 방향만 다름
- 연관관계 주인 (`mappedBy`) — 외래키를 가진 쪽이 주인, `mappedBy` 쪽은 읽기 전용
- 양방향 편의 메서드 — 주인 + 역방향 양쪽 모두 세팅
- 주인 아닌 쪽에만 세팅 시 DB 반영 안 됨 (team_id = null)

**1차 캐시와 연관관계**
- 주인만 세팅 시 DB는 정상이지만 1차 캐시의 역방향 컬렉션은 비어있음
- `em.clear()` 후 재조회 시 DB 기준으로 정상 로딩
- 편의 메서드로 양쪽 세팅 시 1차 캐시도 정상

**지연 로딩 (LAZY) 맛보기**
- `@ManyToOne(fetch = LAZY)` — 연관 엔티티를 실제 사용 시점에 조회
- `em.find(Member)` → SELECT member / `member.team.name` 접근 → SELECT team (2번째 쿼리)
- 자세한 내용은 3. 페치 전략 & N+1 참고

**cascade**
- 부모 엔티티의 상태 변화를 자식에게 전파
- `CascadeType.ALL` — persist/remove/merge 등 모두 전파
- `CascadeType.PERSIST` — persist만 전파
- Order 저장 시 OrderItem 별도 저장 불필요 (cascade = ALL)
- 주의: 자식이 여러 부모에 의해 관리되는 경우 cascade 사용 지양

**orphanRemoval**
- 부모 컬렉션에서 제거된 자식을 자동으로 DELETE
- `orphanRemoval = true` — 컬렉션에서 제거 시 flush 시점에 DELETE
- cascade = ALL + orphanRemoval = true → 부모가 자식의 생명주기를 완전히 관리
- cascade remove — `em.remove(order)` 시 orderItems도 자동 DELETE

**orphanRemoval 주의사항**
- `list.remove(entity)`는 `equals()`로 비교
- Entity에 `equals()` 미정의 시 참조 비교 → `em.clear()` 후 재조회한 객체와 원본은 다른 참조 → 제거 실패
- 안전한 방법:
  ```kotlin
  findOrder.orderItems.removeAt(0)                      // 인덱스로 제거
  findOrder.orderItems.removeIf { it.id == targetId }   // id 기준 제거
  ```

**예제 포인트**
```
- team.members.add(member)만 했을 때 team_id = null 확인
- 주인만 세팅 시 1차 캐시 members.size = 0, DB 재조회 시 1 확인
- changeTeam() 편의 메서드로 1차 캐시도 정상 세팅 확인
- Order만 저장해도 OrderItem 자동 저장 (cascade)
- removeAt(0) → DELETE 확인 / remove(detachedEntity) → 삭제 실패 확인
```

---

## 3. 페치 전략 & N+1 문제

**LAZY vs EAGER**
- `LAZY` — 연관 엔티티를 실제 접근 시점에 SELECT (프록시 객체로 대체)
- `EAGER` — 연관 엔티티를 즉시 JOIN해서 함께 조회
- 기본값
  - `@ManyToOne`, `@OneToOne` → EAGER (기본값이지만 LAZY 권장)
  - `@OneToMany`, `@ManyToMany` → LAZY
- **실무에서는 모든 연관관계 LAZY 원칙** — EAGER는 예상치 못한 쿼리 발생

**N+1 문제**
- 원인: LAZY 로딩 상태에서 컬렉션/연관 엔티티에 반복 접근
- 예시: Member 10명 조회(1번) → 각 member.team 접근(10번) = 총 11번
- EAGER로 바꿔도 해결 안 됨 — JPQL은 fetch 전략 무시하고 우선 실행 후 EAGER면 추가 쿼리
- **해결책**
  1. **Fetch Join** — JPQL에서 join fetch로 한번에 조회
  2. **@EntityGraph** — 어노테이션으로 fetch join 적용
  3. **@BatchSize** — IN 절로 묶어서 조회 (N+1 → 1+1)

**Fetch Join**
```sql
select m from Member m join fetch m.team
```
- 연관 엔티티를 한번의 쿼리로 함께 조회
- 페이징과 컬렉션 fetch join 동시 사용 불가
  - 컬렉션 fetch join + 페이징 → 경고 발생, 메모리에서 페이징 (위험)
  - 해결: `@BatchSize` 또는 `default_batch_fetch_size` 사용

**@EntityGraph**
```kotlin
@EntityGraph(attributePaths = ["team"])
@Query("select m from Member m")       // @Query 없으면 메서드 이름 파싱 시도 → 오류
fun findAllWithTeam(): List<Member>
```
- Fetch Join과 동일한 효과, 어노테이션으로 간편하게 적용
- 복잡한 조건에서는 Fetch Join이 더 유연

**Fetch Join vs EntityGraph JOIN 방식 차이**
- Fetch Join — 기본 **INNER JOIN** → team이 null인 Member 제외됨
- EntityGraph — 기본 **LEFT OUTER JOIN** → team이 null인 Member도 포함
- team null 포함하려면 `left join fetch` 명시 필요
- 설계 의도: Fetch Join은 개발자가 명시적으로 데이터 범위를 결정, EntityGraph는 기존 조회에 fetch만 추가하는 개념

**컬렉션 + 페이징**
- Fetch Join, EntityGraph 모두 컬렉션 + 페이징 시 경고 + 메모리 페이징 (위험)
- 컬렉션 + 페이징이 필요하면 **@BatchSize / default_batch_fetch_size만 해결 가능**

**@BatchSize**
```kotlin
@BatchSize(size = 100)
@OneToMany(mappedBy = "team")
val members: MutableList<Member>
```
- 또는 application.yml에 전역 설정
  ```yaml
  spring.jpa.properties.hibernate.default_batch_fetch_size: 100
  ```
- N+1 → IN 절로 한번에 조회 (1+1 또는 1+N/size)

**LazyInitializationException**
- 영속성 컨텍스트가 닫힌 후 LAZY 연관 엔티티에 접근 시 발생
- 트랜잭션과 영속성 컨텍스트의 생명주기는 항상 같지 않음
- 해결책
  1. Fetch Join / EntityGraph로 미리 로딩
  2. DTO로 변환해서 반환 (엔티티 직접 반환 지양)

**OSIV (Open Session In View)**
- 트랜잭션과 영속성 컨텍스트의 생명주기를 분리하는 설정
- `OSIV = true` (Spring Boot 기본값)
  - 영속성 컨텍스트: HTTP 요청 ~ 응답까지 유지
  - 트랜잭션 종료 후 Controller에서도 LAZY 접근 가능 (읽기만, 변경 감지 동작 안 함)
  - DB 커넥션을 오래 붙잡음 → 트래픽 많으면 커넥션 부족 위험
- `OSIV = false`
  - 영속성 컨텍스트: 트랜잭션과 동일한 범위
  - 트랜잭션 종료 = 영속성 컨텍스트 종료 → Controller에서 LAZY 접근 시 예외
  - DB 커넥션 빨리 반환 → 성능 유리
  - Service에서 DTO 변환 후 반환 필수
- 토비 클린 스프링 방식: OSIV=true 유지 + Controller에서 엔티티 접근해 응답 DTO 생성
- 어느 방식이든 **엔티티가 외부(API 응답)로 직접 노출되지 않으면 됨**

**예제 포인트**
```
- Member 여러 명 조회 후 team.name 접근 → SQL 로그로 N+1 확인
- EAGER로 변경 후 JPQL 조회 → 여전히 N+1 발생 확인
- Fetch Join으로 해결 → 쿼리 1번 확인
- 컬렉션 Fetch Join + 페이징 → 경고 로그 확인
- @BatchSize로 N+1 → IN절 확인
- EntityGraph vs Fetch Join 차이
- @Transactional 없는 상태에서 LAZY 접근 → LazyInitializationException 확인
```

---

## 4. Spring Data JPA Repository

**JpaRepository 계층 구조**
```
Repository
  └── CrudRepository        (save, findById, delete 등 기본 CRUD)
        └── PagingAndSortingRepository  (findAll(Pageable), findAll(Sort))
              └── JpaRepository         (flush, saveAndFlush, deleteInBatch 등)
```

**save() 동작 방식**
- 새 엔티티 (id = 0 or null) → `em.persist()`
- 기존 엔티티 (id 있음) → `em.merge()`
- merge는 SELECT 후 복사 → 성능 주의, 가능하면 변경 감지 사용

**쿼리 메서드**
- 메서드 이름으로 JPQL 자동 생성
- 주요 키워드
  ```
  And, Or, Between, LessThan, GreaterThan, GreaterThanEqual
  Like, StartingWith, EndingWith, Containing
  In, NotIn, IsNull, IsNotNull
  OrderBy, Desc, Asc
  ```
- 조건이 복잡해지면 메서드 이름이 너무 길어짐 → `@Query` 사용

**@Query**
```kotlin
// JPQL
@Query("select m from Member m where m.username = :username")
fun findByUsername(@Param("username") username: String): List<Member>

// Native Query
@Query(value = "select * from member where username = :username", nativeQuery = true)
fun findByUsernameNative(@Param("username") username: String): List<Member>
```

**반환 타입**
- 단건: `Member?` — 없으면 null, 2개 이상이면 예외
- 단건: `Optional<Member>` — 없으면 Optional.empty()
- 다건: `List<Member>` — 없으면 빈 리스트

**Kotlin에서 @Param 생략 가능**
- Kotlin은 컴파일 시 파라미터 이름을 보존 → `@Param` 없이도 동작
- Java는 컴파일 시 파라미터 이름 소실 가능 → `@Param` 필수
- 팀 컨벤션에 따라 명시 여부 결정

**벌크 연산 (@Modifying)**
```kotlin
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update Member m set m.point = m.point + :amount where m.id in :ids")
fun bulkAddPoint(@Param("amount") amount: Int, @Param("ids") ids: List<Long>): Int
```
- 벌크 연산은 영속성 컨텍스트를 거치지 않고 DB 직접 수정
- 벌크 연산 후 1차 캐시와 DB 불일치 발생 → `clearAutomatically = true`로 자동 초기화
- `flushAutomatically = true` — 벌크 연산 전 flush 자동 실행 (같은 트랜잭션에서 변경 감지와 벌크 연산이 섞일 때)
- 실무에서는 변경 감지와 벌크 연산을 같은 트랜잭션에서 섞지 않는 게 원칙

**예제 포인트**
```
- save() 새 엔티티 vs 기존 엔티티 동작 차이 (persist vs merge)
- findByUsernameAndAgeGreaterThan() 쿼리 메서드
- findById() Optional 처리
- @Query JPQL 직접 작성
- @Modifying 벌크 연산 후 캐시 불일치 → clearAutomatically 효과 확인
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
