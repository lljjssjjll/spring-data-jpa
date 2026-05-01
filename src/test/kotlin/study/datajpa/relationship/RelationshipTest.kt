package study.datajpa.relationship

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import study.datajpa.domain.item.Book
import study.datajpa.domain.member.Member
import study.datajpa.domain.order.Order
import study.datajpa.domain.order.OrderItem
import study.datajpa.domain.team.Team
import kotlin.test.Test

@SpringBootTest
@Transactional
class RelationshipTest {

    @Autowired
    lateinit var em: EntityManager

    @Test
    fun `단방향 연관관계`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10, team = team)
        em.persist(member)

        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)

        println("팀 이름: ${findMember.team?.name}")
    }

    @Test
    fun `연관관계 주인 - 주인 아닌 쪽에만 세팅`() {
        val member = Member(username = "member1", age = 10)
        em.persist(member)

        val team = Team(name = "teamA")
        team.members.add(member) // 주인 아닌 쪽에만 세팅
        em.persist(team)

        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)
        println("팀: ${findMember.team?.name}") // null일까 teamA일까?
    }

    @Test
    fun `연관관계 주인 - 주인 쪽에 세팅`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10, team = team)
        em.persist(member)

        em.flush()
        em.clear()

        val findMember = em.find(Member::class.java, member.id)
        println("팀: ${findMember.team?.name}")
    }

    @Test
    fun `주인만 세팅했을 때`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10, team = team)
        em.persist(member)

        // em.flush(), em.clear() 없음 -> 1차 캐시에서 반환

        println("members 크기: ${team.members.size}") // 0일까 1일까?

        em.clear()

        val findTeam = em.find(Team::class.java, team.id)

        println("members 크기: ${findTeam.members.size}") // 0일까 1일까?
    }

    @Test
    fun `양방향 편의 메서드`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10)
        em.persist(member)

        member.changeTeam(team) // 양쪽 모두 세팅

        // em.flush(), em.clear() 없음 -> 1차 캐시에서 반환

        println("members 크기: ${team.members.size}") // 0일까 1일까?
    }

    @Test
    fun `cascade - Order 저장 시 OrderItem 자동 저장`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10, team = team)
        em.persist(member)

        val book = Book(name = "JPA 책", price = 10000, stockQuantity = 10, author = "김영한", isbn = "123")
        em.persist(book)

        val orderItem = OrderItem(item = book, orderPrice = 10000, count = 1)
        val order = Order(member = member)
        order.addOrderItem(orderItem) // orderItem은 persist 안 함

        em.persist(order)

        em.flush()
        em.clear()

        val findOrder = em.find(Order::class.java, order.id)
        println("orderItems 크기: ${findOrder.orderItems.size}")
    }

    @Test
    fun `orphanRemoval - 컬렉션에서 제거 시 자동 DELETE`() {
        val team = Team(name = "teamA")
        em.persist(team)

        val member = Member(username = "member1", age = 10, team = team)
        em.persist(member)

        val book = Book(name = "JPA 책", price = 10000, stockQuantity = 10, author = "김영한", isbn = "123")
        em.persist(book)

        val orderItem = OrderItem(item = book, orderPrice = 10000, count = 1)
        val order = Order(member = member)
        order.addOrderItem(orderItem)
        em.persist(order)

        em.flush()
        em.clear()

        val findOrder = em.find(Order::class.java, order.id)

        println("===== orderItem 제거 =====")
        findOrder.orderItems.removeAt(0)

        em.flush()
    }
}