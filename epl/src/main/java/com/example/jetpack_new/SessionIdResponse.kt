package com.enkod.enkodpushlibrary

data class SessionIdResponse(
    var isActive: Boolean = false,
    var scriptSettings: Any? = null,
    var session_id: String = ""
)