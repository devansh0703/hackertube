package com.github.hackertube.api.ltsync.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerMetaInfo(
    val api: ApiInfo,
    val version: String,
    val capabilities: ServerCapabilities,
)

@Serializable
data class ServerCapabilities(
    val oidc: Boolean,
)

@Serializable
data class ApiInfo(
    @SerialName("hackertube-sync")
    val syncServerVersion: String,
)