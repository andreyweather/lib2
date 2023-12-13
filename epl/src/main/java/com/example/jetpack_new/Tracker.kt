package com.enkod.enkodpushlibrary

import com.google.gson.JsonObject

data class SubscriberInfo(
    val email: String?,
    val phone: String?,
    val firstName: String?,
    val lastName: String?,
    val groups: Array<Any>?,
    val integrations: Array<Int>?,
    val extrafields: HashMap<String,Any?>?,
    val mainChannel: String?
)

data class Product(
        var id: String,
        var categoryId: String?,
        var count: Int?,
        var price: String?,
        var picture: String?
)

data class Order(
    var id: String?,
    var productId: String?,
    var count: Int?,
    var price: String?,
    var picture: String?,
    var sum: Double?
)

data class PageUrl(
    val url: String
)

data class PersonResponse(
    var cart: JsonObject,
    var favourite: JsonObject,
    var session: String,
)