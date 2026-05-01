package study.datajpa.domain.member

import jakarta.persistence.*
import study.datajpa.common.BaseEntity
import study.datajpa.domain.team.Team

@Entity
class Member(
    val username: String,

    var age: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team? = null,

    @Embedded
    var address: Address? = null,

    var point: Int = 0,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity() {

    fun changeTeam(team: Team) {
        this.team = team
        team.members.add(this)
    }
}
