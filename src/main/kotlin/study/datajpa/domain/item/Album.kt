package study.datajpa.domain.item

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("A")
class Album(
    name: String,
    price: Int,
    stockQuantity: Int,
    var artist: String,
    var etc: String? = null,
) : Item(name, price, stockQuantity)
