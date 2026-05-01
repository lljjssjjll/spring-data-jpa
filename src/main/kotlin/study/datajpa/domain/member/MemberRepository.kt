package study.datajpa.domain.member

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MemberRepository : JpaRepository<Member, Long> {
    @EntityGraph(attributePaths = ["team"])
    @Query("select m from Member m")
    fun findAllWithTeam(): List<Member>
}
