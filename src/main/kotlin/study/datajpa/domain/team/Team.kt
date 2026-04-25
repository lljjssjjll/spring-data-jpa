package study.datajpa.domain.team

import jakarta.persistence.*
import study.datajpa.common.BaseEntity
import study.datajpa.domain.member.Member

@Entity
class Team(
    val name: String,

    @OneToMany(mappedBy = "team")
    val members: MutableList<Member> = mutableListOf(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
