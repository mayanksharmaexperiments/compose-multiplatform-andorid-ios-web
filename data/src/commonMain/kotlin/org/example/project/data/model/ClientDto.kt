package org.example.project.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.Client

@Serializable
data class ClientDto(
    val id: Long,
    @SerialName("created_at")
    val createdAt: String? = null,
    val name: String,
    @SerialName("phone_no")
    val phoneNo: String? = null,
    val address: String? = null
)

fun ClientDto.toDomain(): Client = Client(
    id = id,
    name = name,
    phoneNo = phoneNo,
    address = address,
    createdAt = createdAt
)
