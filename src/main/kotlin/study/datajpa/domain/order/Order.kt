package study.datajpa.domain.order

import jakarta.persistence.*
import study.datajpa.common.BaseEntity
import study.datajpa.domain.member.Member

@Entity
@Table(name = "orders") // order는 SQL 예약어
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member,

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.ORDER,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItem> = mutableListOf(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity() {

    fun addOrderItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.order = this
    }

    fun cancel() {
        status = OrderStatus.CANCEL
    }
}
