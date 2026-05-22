package org.example.project.domain.model

data class Client(
    val id: Long,
    val name: String,
    val phoneNo: String? = null,
    val address: String? = null,
    val createdAt: String? = null
)
