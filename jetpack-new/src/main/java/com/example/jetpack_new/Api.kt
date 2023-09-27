package com.enkod.enkodpushlibrary

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface Api {

    @POST("sessions/")
    fun getSessionId(@Header("X-Account")client:String): Call<SessionIdResponse>

    @POST("sessions/start")
    fun startSession(@Header("X-Session-Id")sessionID: String, @Header("X-Account")client:String): Call<SessionIdResponse>

    @PUT("mobile/token")
    @Headers("Content-Type: application/json")
    fun updateToken(
        @Header("X-Account")client:String,
        @Header("X-Session-Id") session:String,
        @Body subscribeBody: SubscribeBody,
    ): Call<UpdateTokenResponse>


    @POST("mobile/subscribe")
    @Headers("Content-Type: application/json")
    fun subscribeToPushToken(
        @Header("X-Account")client:String,
        @Header("X-Session-Id") session:String,
        @Body subscribeBody: SubscribeBody
    ): Call<UpdateTokenResponse>

    @POST("mobile/unsubscribe")
    @Headers("Content-Type: application/json")
    fun unSubscribeToPushToken(
        @Header("X-Account")client:String,
        @Header("X-Session-Id") session:String
    ): Call<UpdateTokenResponse>


    @POST("mobile/click")
    @Headers("Content-Type: application/json")
    fun pushClick(
        @Header("X-Account")client:String,
        @Body pushClickBody: PushClickBody
    ): Call<UpdateTokenResponse>

    @POST("subscribe/")
    fun subscribe(@Header("X-Account")client:String,
                  @Header("X-Session-Id")session:String,
                  @Body subscribeBody:JsonObject)
    : Call<Unit>


    @POST("product/cart")
    fun addToCart(@Header("X-Account")client:String,
                  @Header("X-Session-Id")session:String,
                  @Body body:JsonObject)
            : Call<Unit>

    @POST("product/cart")
    fun removeFromCart(@Header("X-Account")client:String,
                       @Header("X-Session-Id")session:String,
                       @Body body:JsonObject)
            : Call<Unit>

    @POST("product/favourite")
    fun addToFavourite(@Header("X-Account")client:String,
                  @Header("X-Session-Id")session:String,
                  @Body body:JsonObject)
    : Call<Unit>

    @POST("product/favourite")
    fun removeFromFavourite(@Header("X-Account")client:String,
                       @Header("X-Session-Id")session:String,
                       @Body body:JsonObject)
    : Call<Unit>

    @POST("addExtraFields")
    fun addExtrafields(@Header("X-Account")client:String,
                            @Header("X-Session-Id")session:String,
                            @Body body:JsonObject)
    : Call<Unit>

    @POST("page/open")
    fun pageOpen(@Header("X-Account")client:String,
                       @Header("X-Session-Id")session:String,
                       @Body body:PageUrl)
    : Call<Unit>

    @POST("product/open")
    fun productOpen(@Header("X-Account")client:String,
                 @Header("X-Session-Id")session:String,
                 @Body body:JsonObject)
            : Call<Unit>

    @POST("product/order")
    fun order(@Header("X-Account")client:String,
                    @Header("X-Session-Id")session:String,
                    @Body body:JsonObject)
            : Call<Unit>

    @GET("getCartAndFavourite")
    fun checkPerson(@Header("X-Account") client: String,
                    @Header("X-Session-Id") session: String,
                    @QueryMap(encoded = true) params: Map<String, String>)
    : Call<PersonResponse>

    @PUT("updateBySession")
    fun updateContacts(@Header("X-Account") client: String,
                       @Header("X-Session-Id") session: String,
                       @QueryMap(encoded = true) params: Map<String, String>):Call<Unit>

//    @GET("https://ext.enkod.ru/mobile/check")
//    fun checkSubscription(
//        @Header("X-Account") client: String,
//        @Header("X-Session-Id") session: String):Call<Boolean>
}

data class PushClickBody(
    val sessionId: String,
    val personId: Int,
    val messageId: Int,
    val intent: Int,
    val url: String?
)

data class SubscribeBody(
    val sessionId: String,
    val token: String,
    val os: String? = null
)