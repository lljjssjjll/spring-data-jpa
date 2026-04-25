package study.datajpa.domain.item

import jakarta.persistence.*
import study.datajpa.common.BaseEntity

@Entity
class Category(
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: Category? = null,

    @OneToMany(mappedBy = "parent")
    val children: MutableList<Category> = mutableListOf(),

    @OneToMany(mappedBy = "category")
    val categoryItems: MutableList<CategoryItem> = mutableListOf(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()
