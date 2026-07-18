package org.fossify.clock.models

import androidx.annotation.Keep
import kotlinx.serialization.KSerializer

@Keep
@kotlinx.serialization.Serializable
data class Group (
    var id: Int,
    var ref: Int,
    var title: String,
) {
}
