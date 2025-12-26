package Data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RatingX(
    @SerialName("count")
    val count: Int?,
    @SerialName("rate")
    val rate: Double?
)