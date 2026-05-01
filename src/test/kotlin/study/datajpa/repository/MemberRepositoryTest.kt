package study.datajpa.repository

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
class MemberRepositoryTest {

    @Autowired
    lateinit var em: EntityManager

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Test
    @Commit
    fun `save - 새 엔티티 vs 기존 엔티티`() {
        // 새 엔티티 -> persist
        println("===== 새 엔티티 =====")
        val member = Member(username = "member1", age = 10)
        memberRepository.save(member)

        em.flush()
        em.clear()

        // 기존 엔티티 -> merge
        println("===== 기존 엔티티 =====")
        val detachedMember = Member(username = "member2", age = 20, id = member.id)
        memberRepository.save(detachedMember)
    }

    @Test
    fun `쿼리 메서드`() {
        memberRepository.save(Member(username = "member1", age = 10))
        memberRepository.save(Member(username = "member1", age = 20))
        memberRepository.save(Member(username = "member1", age = 30))

        val result = memberRepository.findByUsernameAndAgeGreaterThan("member1", 15)

        result.forEach { println("username: ${it.username}, age: ${it.age}") }
    }

    @Test
    fun `@Query`() {
        memberRepository.save(Member(username = "member1", age = 10))
        memberRepository.save(Member(username = "member2", age = 20))

        val result = memberRepository.findUser("member1", 10)

        result.forEach { println("username: ${it.username}, age: ${it.age}") }
    }

    @Test
    fun `벌크 연산 - 포인트 일괄 지급`() {
        val member1 = memberRepository.save(Member(username = "member1", age = 10))
        val member2 = memberRepository.save(Member(username = "member2", age = 20))
        val member3 = memberRepository.save(Member(username = "member3", age = 30))

        val targetIds = listOf(member1.id, member2.id)

        println("===== 벌크 연산 전 =====")
        println("member1 point: ${member1.point}")

        val updatedCount = memberRepository.bulkAddPoint(1000, targetIds)
        println("업데이트된 건수: $updatedCount")

        println("===== 벌크 연산 후 =====")
        val findMember1 = memberRepository.findById(member1.id).get()
        val findMember3 = memberRepository.findById(member3.id).get()
        println("member1 point: ${findMember1.point}")
        println("member3 point: ${findMember3.point}")
    }
}
