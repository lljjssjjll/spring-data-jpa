package study.datajpa.domain.item

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("B")
class Book(
    name: String,
    price: Int,
    stockQuantity: Int,
    var author: String,
    var isbn: String,
) : Item(name, price, stockQuantity)
