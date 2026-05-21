package org.example.project.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: Long,
    @SerialName("created_at")
    val createdAt: String? = null,
    val name: String,
    @SerialName("phone_no")
    val phoneNo: String? = null,
    val address: String? = null
)
