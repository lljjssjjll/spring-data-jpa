package study.datajpa.domain.item

import jakarta.persistence.*
import study.datajpa.common.BaseEntity

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
abstract class Item(
    var name: String,

    var price: Int,

    var stockQuantity: Int,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
