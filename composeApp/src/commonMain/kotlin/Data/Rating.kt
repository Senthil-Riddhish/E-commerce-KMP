package Data

import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    val count: Int,
    val rate: Double
)