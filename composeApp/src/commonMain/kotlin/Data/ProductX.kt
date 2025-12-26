package Data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductX(
    @SerialName("category")
    val category: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("id")
    val id: Int?,
    @SerialName("image")
    val image: String?,
    @SerialName("price")
    val price: Double?,
    @SerialName("rating")
    val rating: RatingX?,
    @SerialName("title")
    val title: String?
)