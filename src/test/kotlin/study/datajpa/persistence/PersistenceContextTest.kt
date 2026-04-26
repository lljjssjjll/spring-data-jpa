package study.datajpa.persistence

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional
import study.datajpa.domain.member.Member
import study.datajpa.domain.member.MemberRepository
import kotlin.test.Test

@SpringBootTest
@Transactional
class PersistenceContextTest {

    @Autowired
    lateinit var em: EntityManager

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Test
    fun `1차 캐시`() {
        val member = Member(username = "member1", age = 10)

        em.persist(member)
        /*
        GenerationType.IDENTITY 사용으로 persist 즉시 (em.flush() 이전) insert 쿼리가 발생
         */

        em.flush() // DB에 insert
        em.clear() // 1차 캐시 초기화

        println("===== 첫 번째 조회 =====")
        val findMember1 = em.find(Member::class.java, member.id)
        println("===== 두 번째 조회 =====")
        val findMember2 = em.find(Member::class.java, member.id)

        println("동일성: ${findMember1 == findMember2}")
    }

    @Test
    fun `1차 캐시 - flush clear 없음`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)

        // flush, clear 없음

        println("===== 첫 번째 조회 =====")
        val findMember1 = em.find(Member::class.java, member.id)
        println("===== 두 번째 조회 =====")
        val findMember2 = em.find(Member::class.java, member.id)

        println("동일성: ${findMember1 == findMember2}")
    }

    @Test
    fun `동일성 보장`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember1 = em.find(Member::class.java, member.id)
        val findMember2 = em.find(Member::class.java, member.id)

        println("동일성(===): ${findMember1 === findMember2}")
        println("동등성(==): ${findMember1 == findMember2}")
    }

    @Test
    fun `clear 후 동일성`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember1 = em.find(Member::class.java, member.id)

        em.clear()

        val findMember2 = em.find(Member::class.java, member.id)

        println("동일성(===): ${findMember1 === findMember2}")
        println("동등성(==): ${findMember1 == findMember2}")
    }

    @Test
    fun `변경 감지`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)

        println("==== age 변경 ====")
        findMember.age = 20 // save() 없음

        println("==== flush 전 ====")
        em.flush()
        println("==== flush 후 ====")
    }

    @Test
    @Commit
    fun `변경 감지 - flush 없음`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)
        findMember.age = 20 // flush 없음, save 없음

        println("==== 트랜잭션 종료 ====")
    }

    @Test
    fun `JPQL 실행 전 flush`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)
        findMember.age = 20 // 변경, flush 없음

        println("==== JPQL 실행 ====")
        val result = em.createQuery("select m from Member m where m.id = :id", Member::class.java)
            .setParameter("id", member.id)
            .singleResult

        println("조회된 age: ${result.age}") // 20이 나올까 20이 나올까?
    }

    @Test
    fun `JPQL - 전체 조회`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)
        findMember.age = 20 // 변경, flush 없음

        println("==== JPQL 전체 조회 ====")
        em.createQuery("select m from Member m", Member::class.java).resultList
    }

    @Test
    fun `준영속 - 변경 감지 동작 안 함`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()

        em.detach(member) // 준영속 상태로 전환

        println("==== age 변경 ====")
        member.age = 20 // 변경 감지 동작할까?

        em.flush()
        println("==== flush 후 ====")
    }

    @Test
    fun `준영속 - clear`() {
        val member1 = Member(username = "member1", age = 10)
        val member2 = Member(username = "member2", age = 10)
        em.persist(member1)
        em.persist(member2)
        em.flush()

        em.clear() // 전체 준영속

        member1.age = 20
        member2.age = 20

        em.flush()
    }

    @Test
    fun `준영속 - merge`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.detach(member) // 준영속

        member.age = 20

        println("===== merge =====")
        val merged = em.merge(member)

        println("member 영속 여부: ${em.contains(member)}")
        println("merged 영속 여부: ${em.contains(merged)}")

        em.flush()
    }

    @Test
    @Commit
    fun `삭제 - remove`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)
        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)

        println("==== remove ====")
        em.remove(findMember)

        println("==== flush ====")
        em.flush()
    }
}
