package study.datajpa.fetch

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import study.datajpa.domain.member.Member
import study.datajpa.domain.member.MemberRepository
import study.datajpa.domain.team.Team
import kotlin.test.Test

@SpringBootTest
@Transactional
class FetchStrategyTest {

    @Autowired
    lateinit var em: EntityManager

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Test
    fun `N+1 문제 재현`() {
        val teamA = Team(name = "teamA")
        val teamB = Team(name = "teamB")
        em.persist(teamA)
        em.persist(teamB)

        em.persist(Member(username = "member1", age = 10, team = teamA))
        em.persist(Member(username = "member2", age = 20, team = teamA))
        em.persist(Member(username = "member3", age = 30, team = teamB))
        em.persist(Member(username = "member4", age = 40, team = teamB))

        em.flush()
        em.clear()

        println("===== JPQL 실행 =====")
        val members = em.createQuery("select m from Member m", Member::class.java).resultList

        println("===== team 접근 =====")
        members.forEach { println("username: ${it.username}, team: ${it.team?.name}") }
    }

    @Test
    fun `Fetch Join으로 N+1 해결`() {
        val teamA = Team(name = "teamA")
        val teamB = Team(name = "teamB")
        em.persist(teamA)
        em.persist(teamB)

        em.persist(Member(username = "member1", age = 10, team = teamA))
        em.persist(Member(username = "member2", age = 20, team = teamB))
        em.persist(Member(username = "member3", age = 30, team = teamA))
        em.persist(Member(username = "member4", age = 40, team = teamB))

        em.flush()
        em.clear()

        println("===== Fetch Join JPQL 실행 =====")
        val members = em.createQuery("select m from Member m join fetch m.team", Member::class.java).resultList

        println("===== team 접근 =====")
        members.forEach { println("username: ${it.username}, team: ${it.team?.name}") }
    }

    @Test
    fun `컬렉션 Fetch Join + 페이징`() {
        val teamA = Team(name = "teamA")
        em.persist(teamA)

        em.persist(Member(username = "member1", age = 10, team = teamA))
        em.persist(Member(username = "member2", age = 20, team = teamA))
        em.persist(Member(username = "member3", age = 30, team = teamA))

        em.flush()
        em.clear()

        println("===== 컬렉션 Fetch Join + 페이징 =====")
        val teams = em.createQuery("select t from Team t join fetch t.members", Team::class.java)
            .setFirstResult(0)
            .setMaxResults(2)
            .resultList

        teams.forEach { println("team: ${it.name}, members: ${it.members.size}") }
    }

    @Test
    fun `BatchSize로 컬렉션 페이징 해결`() {
        val teamA = Team(name = "teamA")
        val teamB = Team(name = "teamB")
        em.persist(teamA)
        em.persist(teamB)

        em.persist(Member(username = "member1", age = 10, team = teamA))
        em.persist(Member(username = "member2", age = 20, team = teamA))
        em.persist(Member(username = "member3", age = 30, team = teamB))
        em.persist(Member(username = "member4", age = 40, team = teamB))

        em.flush()
        em.clear()

        println("===== Team 페이징 =====")
        val teams = em.createQuery("select t from Team t", Team::class.java)
            .setFirstResult(0)
            .setMaxResults(2)
            .resultList

        println("===== members 접근 =====")
        teams.forEach { println("team: ${it.name}, members:${it.members.size}") }
    }

    @Test
    fun `EntityGraph`() {
        val teamA = Team(name = "teamA")
        val teamB = Team(name = "teamB")
        em.persist(teamA)
        em.persist(teamB)

        em.persist(Member(username = "member1", age = 10, team = teamA))
        em.persist(Member(username = "member2", age = 20, team = teamA))
        em.persist(Member(username = "member3", age = 30, team = teamB))
        em.persist(Member(username = "member4", age = 40, team = teamB))

        em.flush()
        em.clear()

        println("===== EntityGraph =====")
        val members = memberRepository.findAllWithTeam()

        members.forEach { println("username: ${it.username}, team: ${it.team?.name}") }
    }
}
