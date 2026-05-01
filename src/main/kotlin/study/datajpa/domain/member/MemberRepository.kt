package study.datajpa.domain.member

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberRepository : JpaRepository<Member, Long> {
    @EntityGraph(attributePaths = ["team"])
    @Query("select m from Member m")
    fun findAllWithTeam(): List<Member>

    fun findByUsernameAndAgeGreaterThan(username: String, age: Int): List<Member>

    @Query("select m from Member m where m.username = :username and m.age = :age")
    fun findUser(@Param("username") username: String, @Param("age") age: Int): List<Member>

    @Modifying(clearAutomatically = true)
//    @Modifying(clearAutomatically = false)
    @Query("update Member m set m.point = m.point + :amount where m.id in :ids")
    fun bulkAddPoint(amount: Int, ids: List<Long>): Int
}
