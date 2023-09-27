package com.enkod.enkodpushlibrary

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.example.jetpack_new.BuildConfig
import com.example.jetpack_new.R
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit


object EnkodPushLibrary {

    class Builder {

        fun build(applicationContext: Context, account: String) {
            init(applicationContext, account)
        }

        fun setOnTokenListener(function: (String) -> Unit): Builder {
            setOnNewTokenCallback(function)
            return this
        }

        fun setOnPushClickListener(function: (Bundle, String) -> Unit): Builder {
            setOnPushClickCallback(function)
            return this
        }

        fun setOnDynamicLinkClickListener(function: ((String) -> Unit)? = null): Builder {
            setOnDynamicLinkCallback(function)
            return this
        }

        fun setOnMessageReceiveListener(function: (RemoteMessage) -> Unit): Builder {
            setOnMessageReceiveCallback(function)
            return this
        }

        fun setOnDeletedMessageListener(function: () -> Unit): Builder {
            setOnDeletedMessage(function)
            return this
        }

        fun setOnProductCallBackListener(function: (String) -> Unit): Builder{
            setOnProductCallback(function)
            return this
        }

        fun setOnErrorListener(function: (String) -> Unit): Builder{
            setOnErrorCallback(function)
            return this
        }

        fun setOnBaseUrlCheckerListener(function: (String) -> String): Builder{
            setOnBaseUrlCheckerCallback(function)
            return this
        }
    }

    class NullOnEmptyConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            val delegate: Converter<ResponseBody, *> =
                retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter { body ->
                if (body.contentLength() == 0L) null else delegate.convert(
                    body
                )
            }
        }
    }
    enum class OpenIntent {
        DYNAMIC_LINK, OPEN_URL, OPEN_APP;

        fun get(): String {
            return when (this) {
                DYNAMIC_LINK -> "0"
                OPEN_URL -> "1"
                OPEN_APP -> "2"
            }
        }

        companion object {
            fun get(intent: String?): OpenIntent {
                return when (intent) {
                    "0" -> DYNAMIC_LINK
                    "1" -> OPEN_URL
                    "2" -> OPEN_APP
                    else -> OPEN_APP
                }
            }
        }
    }

    private var isSessionStarted: Boolean = false
    private const val NOTIFICATION_ID = 1
    internal val vibrationPattern = longArrayOf(1500, 500)
    internal val defaultIconId: Int = R.drawable.ic_android_black_24dp
    private const val TAG = "EnkodPushLibrary"
    private const val SESSION_ID_TAG: String = "${TAG}_SESSION_ID"
    private const val TOKEN_TAG: String = "${TAG}_TOKEN"
    private const val ACCOUNT_TAG: String = "${TAG}_ACCOUNT"
    private const val EMAIL_TAG: String = "${TAG}_EMAIL"
    private const val PHONE_TAG: String = "${TAG}_PHONE"

    internal lateinit var soundOn: String
    internal lateinit var vibrationOn: String
    internal lateinit var pushShowTime: String
    internal lateinit var ledColor: String
    internal lateinit var ledOnMs: String
    internal lateinit var ledOffMs: String
    internal lateinit var colorName: String
    internal lateinit var iconRes: String
    internal lateinit var notificationPriority: String
    internal lateinit var bannerName: String
    internal lateinit var intentName: String
    internal lateinit var channelId: String
    internal lateinit var notificationImage: String
    internal lateinit var bigImageUrl: String
    internal lateinit var url: String
    internal lateinit var title: String
    internal lateinit var body: String
    internal lateinit var notificationImportance: String
    internal lateinit var actionButtonsUrl: String
    internal lateinit var actionButtonText: String
    internal lateinit var actionButtonIntent: String
    internal lateinit var personId: String
    internal lateinit var messageId: String
    internal lateinit var account: String
    internal var email: String? = null
    internal var phone: String? = null
    internal var loggedIn: Boolean = false

    internal var Cart = JsonObject()
    internal var Favourite = JsonObject()

    private var token: String? = null
    private var sessionId: String? = null
    private var callback: (RemoteMessage) -> Unit = {}
    private var onPushClickCallback: (Bundle, String) -> Unit = { _, _ -> }
    private var onDynamicLinkClick: ((String) -> Unit)? = null
    private var newTokenCallback: (String) -> Unit = {}
    private var onDeletedMessage: () -> Unit = {}

    private var onProductActionCallback: (String) -> Unit = {}
    private var onSubscriberCallback: (String) -> Unit = {}

    private var onErrorCallback: (String) -> Unit = {}

    private var onBaseUrlCheckerCallback: (String) -> String = { "" }

    private lateinit var retrofit: Api
    private lateinit var client: OkHttpClient

    private fun init(ctx: Context, account: String) {
        initRetrofit()

        soundOn = ctx.getString(R.string.sound_on)
        vibrationOn = ctx.getString(R.string.vibration_on)
        pushShowTime = ctx.getString(R.string.push_show_time)
        ledColor = ctx.getString(R.string.led_color)
        ledOnMs = ctx.getString(R.string.led_on_ms)
        ledOffMs = ctx.getString(R.string.led_of_ms)
        colorName = ctx.getString(R.string.color_name)
        iconRes = ctx.getString(R.string.icon_res)
        notificationPriority = ctx.getString(R.string.notification_priority)
        bannerName = ctx.getString(R.string.banner_name)
        intentName = ctx.getString(R.string.intent_name)
        channelId = ctx.getString(R.string.channel_id)
        notificationImage = ctx.getString(R.string.notification_image)
        bigImageUrl = ctx.getString(R.string.big_image_url)
        title = ctx.getString(R.string.title)
        body = ctx.getString(R.string.body)
        url = ctx.getString(R.string.url)
        notificationImportance = ctx.getString(R.string.notification_importance)
        actionButtonsUrl = ctx.getString(R.string.action_buttons_url)
        actionButtonText = ctx.getString(R.string.action_button_text)
        actionButtonIntent = ctx.getString(R.string.action_button_intent)
        personId = ctx.getString(R.string.person_id)
        messageId = ctx.getString(R.string.message_id)

        setClientName(account)
        getSessionId(ctx)

    }

    private fun initRetrofit() {
        client = OkHttpClient.Builder()
            .callTimeout(60L, TimeUnit.SECONDS)
            .connectTimeout(60L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

        var baseUrl: String
        onBaseUrlCheckerCallback("").let{
            baseUrl = it.ifEmpty {
                BuildConfig.BASE_URL
            }
        }

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(Api::class.java)
    }

    fun setOnMessageReceiveCallback(callback: (RemoteMessage) -> Unit) {
        this.callback = callback
    }

    fun setOnPushClickCallback(callback: (Bundle, String) -> Unit) {
        this.onPushClickCallback = callback
    }

    fun setOnNewTokenCallback(callback: (String) -> Unit) {
        this.newTokenCallback = callback
    }

    fun setOnDynamicLinkCallback(callback: ((String) -> Unit)?) {
        this.onDynamicLinkClick = callback
    }

    fun setOnDeletedMessage(callback: () -> Unit) {
        onDeletedMessage = callback
    }

    fun setOnProductCallback(callback: (String) -> Unit){
        this.onProductActionCallback = callback
    }

    fun setOnErrorCallback(callback: (String) -> Unit){
        this.onErrorCallback = callback
    }

    fun setOnBaseUrlCheckerCallback(callback: (String) -> String){
        this.onBaseUrlCheckerCallback = callback
    }
    fun logIn(ctx: Context){
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseInstallations.getInstance(FirebaseApp.getInstance()).getToken(true)
        //subscribeToPush{}
    }

    fun logOut(ctx: Context){
        unsubscribeFromPush{}
        dropEmail(ctx)
        dropPhone(ctx)
        dropSession(ctx)
        setLoggedOut(ctx)
        clearCartAndFavourite()
        getSessionId(ctx) //в процессе логаута чистится вся информация по пользователю и создается новая сессия
    }

    internal fun onNewToken(context: Context, token: String) {
        if (this.token != token) {
            saveToken(context, token)
        }
        if (isSessionStarted) {
            updateToken(context)
        } else {
            startSession()
        }
    }

    private fun updateToken(ctx:Context) {
        retrofit.updateToken(
            getClientName(),
            getSession(),
            SubscribeBody(
                sessionId = sessionId!!,
                token = token!!
            )
        ).enqueue(object : Callback<UpdateTokenResponse> {
            override fun onResponse(
                call: Call<UpdateTokenResponse>,
                response: Response<UpdateTokenResponse>
            ) {
                logInfo("token updated")
                newTokenCallback(token!!)
                subscribeToPush {}
            }

            override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                logInfo("token update failure")
            }

        })
    }

    private fun saveToken(ctx: Context, t: String) {
        logInfo("on save token")
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit()
            .putString(TOKEN_TAG, t)
            .apply()
        this.token = t
        newTokenCallback(t)
    }

    private fun startSession() {
        token?.let {
            logInfo("on start session \n")
            sessionId?.let { it1 ->
                retrofit.startSession(it1, getClientName()).enqueue(object : Callback<SessionIdResponse> {
                    override fun onResponse(
                        call: Call<SessionIdResponse>,
                        response: Response<SessionIdResponse>
                    ) {
                        logInfo("session started ${response.body()?.session_id}")
                        isSessionStarted = true
                        newTokenCallback(it)
                        subscribeToPush {}
                    }
                    override fun onFailure(call: Call<SessionIdResponse>, t: Throwable) {
                        logInfo("session not started ${t.message}")
                        newTokenCallback(it)
                    }
                })
            }
        }
    }

    private fun setEmail(ctx: Context, e: String){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().putString(EMAIL_TAG, e).apply()
        email = e
    }

    private fun setPhone(ctx: Context, p: String){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().putString(PHONE_TAG, p).apply()
        phone = p
    }

    private fun setLoggedIn(ctx: Context) {
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().putBoolean("loggedIn", true).apply()
        loggedIn = true
    }

    private fun dropEmail(ctx: Context){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().remove(EMAIL_TAG).apply()
        email = ""
    }

    private fun dropPhone(ctx: Context){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().remove(PHONE_TAG).apply()
        phone = ""
    }

    private fun setLoggedOut(ctx: Context){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().putBoolean("loggedIn", false).apply()
        loggedIn = false
    }

    private fun dropSession(ctx: Context){
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().remove(SESSION_ID_TAG).apply()
        preferences.edit().remove(TOKEN_TAG).apply()
        sessionId = ""
        token = ""
    }

    private fun clearCartAndFavourite(){
        Cart = JsonObject()
        Favourite = JsonObject()
    }

    private fun onMessageReceived(message: RemoteMessage) {
        callback.invoke(message)
    }

    internal fun processMessage(context: Context, message: RemoteMessage) {
        onMessageReceived(message)
        if(context.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean("loggedIn", false)){
            createAndShowNotification(context, message)
        }
    }

    private fun createAndShowNotification(context: Context, message: RemoteMessage) {
        val builder = createNotification(context, message)
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showAndroidONotification(
                notificationManager,
                message.data
            )
        }
        notificationManager?.notify(NOTIFICATION_ID, builder)
        setShowPushWindow(notificationManager, message.data[pushShowTime]?.toLongOrNull())
    }

    private fun setShowPushWindow(notificationManager: NotificationManager?, pushShowTime: Long?) {
        if (pushShowTime != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager?.cancel(NOTIFICATION_ID)
            }, pushShowTime)
        }
    }

    internal fun logInfo(msg: String) {
        Log.i(TAG, msg)
    }

    private fun createNotification(context: Context, message: RemoteMessage): Notification {
        with(message.data) {
            val builder = NotificationCompat.Builder(context, get(channelId)!!)

            logInfo(
                message.data.toString()
            )

            val pendingIntent: PendingIntent = getIntent(context, message.data)
            builder
                .setIcon(context, message.notification?.icon)
                .setColor(context, message.notification?.color)
                .setLargeIcon(message.notification?.imageUrl)
                .setStyle(message)
                .setLights(get(ledColor), get(ledOnMs), get(ledOffMs))
                .setVibrate(get(vibrationOn).toBoolean())
                .setSound(get(soundOn).toBoolean())
                .setContentTitle(message.notification?.title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addActions(context, message.data)
                .priority = setPriority(get(notificationPriority))

            return builder.build()
        }
    }

    private fun setPriority(priority: String?): Int {
        return priority?.toInt() ?: NotificationCompat.PRIORITY_DEFAULT
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getIntent(context: Context, data: Map<String, String>): PendingIntent {
        val intent = when (OpenIntent.get(data[intentName])) {
            OpenIntent.OPEN_URL -> {
                getOpenUrlIntent(context, data)
            }
            OpenIntent.DYNAMIC_LINK -> {
                getDynamicLinkIntent(context, data)
            }
            else -> {
                getOpenAppIntent(context)
            }
        }
        intent.putExtras(
            bundleOf(
                personId to data[personId],
                messageId to data[messageId],
            )
        )
        intent.putExtra(personId, data[personId])
        intent.putExtra(messageId, data[messageId])

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            Random().nextInt(1000),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun getIntent(
        context: Context,
        data: Map<String, String>,
        field: String,
        url: String
    ): PendingIntent {
        val intent = when (OpenIntent.get(field)) {
            OpenIntent.OPEN_URL -> {
                getOpenUrlIntent(context, url)
            }
            OpenIntent.DYNAMIC_LINK -> {
                getDynamicLinkIntent(context, url)
            }
            else -> {
                getOpenAppIntent(context)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(personId, data[personId])
        intent.putExtra(messageId, data[messageId])

        return PendingIntent.getActivity(
            context,
            Random().nextInt(1000),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun getOpenAppIntent(context: Context): Intent {
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.OPEN_APP.get(),
                    OpenIntent.OPEN_APP.name to true
                )
            )
        }
    }

    internal fun getPackageLauncherIntent(context: Context): Intent? {
        val pm: PackageManager = context.packageManager
        return pm.getLaunchIntentForPackage(context.packageName)
    }

    private fun getDynamicLinkIntent(context: Context, data: Map<String, String>): Intent {
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.DYNAMIC_LINK.get(),
                    OpenIntent.OPEN_APP.name to true,
                    url to data[url]
                )
            )
        }
    }

    private fun getDynamicLinkIntent(context: Context, url: String): Intent {
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.DYNAMIC_LINK.get(),
                    OpenIntent.OPEN_APP.name to true,
                    this.url to url
                )
            )
        }
    }

    private fun getOpenUrlIntent(context: Context, data: Map<String, String>): Intent {
        logInfo("GET INTENT ${OpenIntent.get(data[intentName])} ${data[intentName]} ${data[url]}")
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtra(intentName, OpenIntent.OPEN_URL.get())
            it.putExtra(url, data[url])
            it.putExtra(OpenIntent.OPEN_APP.name, true)
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.OPEN_URL.get(),
                    OpenIntent.OPEN_APP.name to true,
                    url to data[url]
                )
            )
        }
    }

    private fun getClientName():String{
        return this.account
    }

    private fun getSession():String{
        return if(!this.sessionId.isNullOrEmpty()){
            this.sessionId!!
        } else{
            ""
        }
    }

    private fun setClientName(acc:String){
        val context = FirebaseApp.getInstance().applicationContext
        val preferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences
            .edit()
            .putString(ACCOUNT_TAG, acc)
            .apply()

        this.account = acc
    }

    private fun getOpenUrlIntent(context: Context, url: String): Intent {
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.OPEN_URL.get(),
                    OpenIntent.OPEN_APP.name to true,
                    this.url to url
                )
            )
        }
    }

    internal fun getResourceId(
        context: Context,
        pVariableName: String?,
        resName: String?,
        pPackageName: String?
    ): Int {
        return try {
            context.resources.getIdentifier(pVariableName, resName, pPackageName)
        } catch (e: Exception) {
            e.printStackTrace()
            defaultIconId
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAndroidONotification(
        notificationManager: NotificationManager?,
        data: Map<String, String>
    ) {
        val channelID = data[channelId]
        val hasSound = data[soundOn].toBoolean()
        val hasVibration = data[vibrationOn].toBoolean()

        val importance = setImportance(data[notificationPriority])
        notificationManager?.notificationChannels?.forEach {
            if (it.id.contains("enkod_lib")) {
                notificationManager.deleteNotificationChannel(it.id)
            }
        }

        val channel = NotificationChannel(channelID, channelID, importance).apply {
            description = channelID
            this.vibrationPattern = set(hasVibration)
            setSound(hasSound)
            enableLights(data[ledColor])
            enableVibration(true)
        }

        notificationManager?.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setImportance(s: String?): Int {
        val importance = s?.toIntOrNull()
        return if (importance != null) {
            return when (importance) {
                (-2) -> NotificationManager.IMPORTANCE_MIN
                (-1) -> NotificationManager.IMPORTANCE_LOW
                (0) -> NotificationManager.IMPORTANCE_DEFAULT
                else -> NotificationManager.IMPORTANCE_HIGH
            }
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }
    }

    internal fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            try{
                connection.connect()
            } catch (e: Exception){
                Log.e("awesome", "Error in getting connection: " + e.localizedMessage)
            }
            //connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e("awesome", "Error in getting notification image: " + e.localizedMessage)
            null
        }
    }

    internal fun onDeletedMessage() {
        onDeletedMessage.invoke()
    }

    internal fun set(hasVibration: Boolean): LongArray {
        return if (hasVibration) {
            vibrationPattern
        } else {
            longArrayOf(0)
        }
    }

    fun handleExtras(context: Context, extras: Bundle) {
        val link = extras.getString(url)
        Log.i("handleExtras", "${extras.getString("messageId")}")
        sendPushClickInfo(extras)
        when (OpenIntent.get(extras.getString(intentName))) {
            OpenIntent.OPEN_URL -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(link))
                )
            }
            OpenIntent.DYNAMIC_LINK -> {
                link?.let{
                    onDynamicLinkClick?.let { callback ->
                        return callback(it)
                    }
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(link))
                    )
                }
            }
            else -> {
                context.startActivity(getPackageLauncherIntent(context))
            }
        }
    }

    fun subscribeToPush(callback: (String) -> Unit) {
        retrofit.subscribeToPushToken(
            getClientName(),
            getSession(),
            SubscribeBody(
                sessionId = sessionId!!,
                token = token!!,
                os = "android"
            )
        ).enqueue(object : Callback<UpdateTokenResponse> {
            override fun onResponse(
                call: Call<UpdateTokenResponse>,
                response: Response<UpdateTokenResponse>
            ) {
                logInfo("subscribed")
                callback("subscribed")
            }

            override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                logInfo("MESSAGE ${t.localizedMessage}")
                callback("failure")
            }

        })
    }

    fun unsubscribeFromPush(callback: (String) -> Unit) {
        retrofit.unSubscribeToPushToken(
            getClientName(),
            getSession()
        ).enqueue(object : Callback<UpdateTokenResponse> {
            override fun onResponse(
                call: Call<UpdateTokenResponse>,
                response: Response<UpdateTokenResponse>
            ) {
                logInfo("unsubscribed")
                callback("unsubscribed")
            }

            override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                logInfo("failure")
                callback("failure")
            }

        })
    }

    private fun getSessionId(ctx: Context) {
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        sessionId = preferences.getString(SESSION_ID_TAG, null)
        token = preferences.getString(TOKEN_TAG, null)
        logInfo("get local session $sessionId \n$token\n")
        if (sessionId.isNullOrEmpty()) {
            getSessionIdFromApi(ctx)
        } else {
            getFirebaseToken(ctx)
        }
    }

    private fun getFirebaseToken(context: Context) {
        FirebaseMessaging.getInstance().apply {
            token.addOnCompleteListener {
                it.result?.let { it1 ->
                    logInfo("get firebase token $it1\n")
                    onNewToken(context, it1)
                }
            }
        }
    }

    private fun getSessionIdFromApi(ctx: Context) {
        retrofit.getSessionId(getClientName()).enqueue(object : Callback<SessionIdResponse> {
            override fun onResponse(
                call: Call<SessionIdResponse>,
                response: Response<SessionIdResponse>
            ) {
                response.body()?.session_id?.let {
                    logInfo("get token from api $it\n")
                    saveSessionId(ctx, it)
                    Toast.makeText(ctx, "connect_getSessionIdFromApi", Toast.LENGTH_LONG).show()
                } ?: run {
                    logInfo("get token from api error")
                    Toast.makeText(ctx, "error_getSessionIdFromApi", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SessionIdResponse>, t: Throwable) {
                logInfo("get token from api failure ${t.message}")
                Toast.makeText(ctx, "error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveSessionId(ctx: Context, sessId: String) {
        logInfo("save session id")
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences
            .edit()
            .putString(SESSION_ID_TAG, sessId)
            .apply()
        this.sessionId = sessId
        getFirebaseToken(ctx)
    }

    private fun sendPushClickInfo(extras: Bundle) {
        if (extras.getString(personId) != null && extras.getString(messageId) != null) {
            retrofit.pushClick(
                getClientName(),
                PushClickBody(
                    sessionId = sessionId!!,
                    personId = extras.getString(personId, "0").toInt(),
                    messageId = extras.getString(messageId, "-1").toInt(),
                    intent = extras.getString(intentName, "2").toInt(),
                    url = extras.getString(url)
                )
            ).enqueue(object : Callback<UpdateTokenResponse> {
                override fun onResponse(
                    call: Call<UpdateTokenResponse>,
                    response: Response<UpdateTokenResponse>
                ) {
                    val msg = "succsess"
                    logInfo(msg)
                    onPushClickCallback(extras, msg)
                }

                override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                    val msg = "failure"
                    logInfo(msg)
                    onPushClickCallback(extras, msg)
                }
            })
        }
    }

    class Subscriber {
        private fun setToJson(obj: JsonObject, m: Map.Entry<String, Any>){
            when (m.value) {
                is Int -> obj.addProperty(m.key, m.value.toString().toInt())
                is Boolean -> obj.addProperty(m.key, m.value.toString().toBoolean())
                is Float -> obj.addProperty(m.key, m.value.toString().toFloat())
                else -> obj.addProperty(m.key, m.value.toString())
            }
        }

        fun Subscribe(ctx: Context, sub:SubscriberInfo) {
            if ( loggedIn ){ return }
            if (sessionId == ""){
                logInfo("session is empty")
                return
            }

            var mainChannel = sub.mainChannel
            if(!sub.email.isNullOrEmpty() && !sub.phone.isNullOrEmpty() && sub.mainChannel.isNullOrEmpty()){
               mainChannel = "email"
            }

            val efObject = JsonObject()
            if (!sub.extrafields.isNullOrEmpty()){
                for (o in sub.extrafields.iterator()){
                    val v = o.value
                    when (o.value) {
                        is Int -> efObject.addProperty(o.key, v.toString().toInt())
                        is Boolean -> efObject.addProperty(o.key, v.toString().toBoolean())
                        is Float -> efObject.addProperty(o.key, v.toString().toFloat())
                        else -> efObject.addProperty(o.key, v.toString())
                    }
                }
            }

            val req = JsonObject()

            req.add("extraFileds", efObject)
            req.add("integrations", Gson().toJsonTree(sub.integrations))
            req.add("groups", Gson().toJsonTree(sub.groups))
            req.add("mainChannel", Gson().toJsonTree(mainChannel))

            val fileds = JsonObject()
            if (!sub.email.isNullOrEmpty()) {fileds.addProperty("email", sub.email)}
            if (!sub.phone.isNullOrEmpty()) {fileds.addProperty("phone", sub.phone)}
            if (!sub.firstName.isNullOrEmpty()) {fileds.addProperty("firstName", sub.firstName)}
            if (!sub.lastName.isNullOrEmpty()) {fileds.addProperty("lastName", sub.lastName)}
            req.add("fields", fileds)

            val params = hashMapOf<String, String>()
            if (!sub.email.isNullOrEmpty()) {params.put("email", sub.email)}
            if (!sub.phone.isNullOrEmpty()) {params.put("phone", sub.phone)}

            retrofit.checkPerson(
                getClientName(),
                getSession(),
                params,
            ).enqueue(object : Callback<PersonResponse>{
                override fun onResponse(call: Call<PersonResponse>, response: Response<PersonResponse>) {
                    if(response.code() == HTTP_NOT_FOUND){
                        subscribeNew(ctx, req)
                    }else{
                        val body = response.body()
                        if (body != null && !sessionId.isNullOrEmpty()) {
                            isSessionStarted = true
                            subscribeNew(ctx, req)
                            saveSessionId(ctx, getSession())
                            setLoggedIn(ctx)
                            setEmail(ctx, sub.email?: "")
                            setPhone(ctx, sub.phone?: "")
                            mergeCarts(body.cart)
                            mergeFavourites(body.favourite)
                        }
                    }
                }
                override fun onFailure(call: Call<PersonResponse>, t: Throwable) {
                    //println(t.message)
                    logInfo("error when subscribing")
                    //onSubscribeCallback
                }
            })
        }

        fun parseOldStruct(oldCart: JsonObject): ArrayList<Product>? {
            val products = if(oldCart.getAsJsonArray("products") != null){
                  oldCart.getAsJsonArray("products")
            }else{
                return null
            }

            val response = arrayListOf<Product>()

            for(i in 0 until products.count()){
                val product = products[i].asJsonObject

                var id = ""
                var groupId = ""
                var count = 0
                if (product.get("productId").asString != ""){
                    id = product.get("productId").asString
                    product.remove("id")
                }
                try{
                    groupId = product.get("groupId").asString
                    product.remove("groupId")
                }catch (_: Exception){}

                if (product.get("count").asInt != 0){
                    count = product.get("count").asInt
                    product.remove("count")
                }

                val fields = mutableMapOf<String,Any>()
                product.keySet().forEach{
                    if (product.get(it) == null){
                        fields[it] = product.get(it)
                    }
                }

                response.add(Product(
                    id = id,
                    groupId = groupId,
                    count = count,
                    fields = fields
                ))
            }
            return response
        }

        fun mergeCarts(oldCart: JsonObject){
            val productsArray = parseOldStruct(oldCart) ?: return

            for (product in productsArray){
                val new = JsonObject().apply { addProperty("productId", product.id) }
                if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
                val count = if(product.count == 0) { 1 } else { product.count!! }
                new.addProperty("count", count)
                for (ef in product.fields.orEmpty()){
                    ProductActions().setValue(new, ef)
                }

                val products = if(Cart.getAsJsonArray("products") != null){
                    Cart.getAsJsonArray("products")
                }else{
                    JsonArray()
                }

                var exist = false
                val productsDuplicate = products.deepCopy()
                for(p in 0 until products.count()){
                    val current = productsDuplicate[p].asJsonObject
                    if (current.getAsJsonPrimitive("productId").asString == product.id){
                        exist = true
                        current.addProperty("count", current.get("count").asInt + count)
                        new.keySet().forEach{
                            if (current.get(it) == null){
                                ProductActions().setToJson(current, JsonPrimitive(it).asString, new.getAsJsonPrimitive(it).asString)
                            }
                        }
                        productsDuplicate[p] = current
                        break
                    }
                }
                if (!exist) {productsDuplicate.add(new)}

                Cart.add("products", productsDuplicate)
            }
            val noStat = if(Cart.getAsJsonArray("products") != null){
                Cart.getAsJsonArray("products")
            }else{
                JsonArray()
            }
            retrofit.addToCart(getClientName(), getSession(), JsonObject().apply {
                add("cart", JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", noStat)
                    })
            }).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun mergeFavourites(oldFavourite: JsonObject){
            val productsArray = parseOldStruct(oldFavourite) ?: return

            for (product in productsArray){
                val new = JsonObject().apply { addProperty("productId", product.id) }
                if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
                val count = if(product.count == 0) { 1 } else { product.count!! }
                new.addProperty("count", count)
                for (ef in product.fields.orEmpty()){
                    ProductActions().setValue(new, ef)
                }

                val products = if(Favourite.getAsJsonArray("products") != null){
                    Favourite.getAsJsonArray("products")
                }else{
                    JsonArray()
                }

                var exist = false
                val productsDuplicate = products.deepCopy()
                for(p in 0 until products.count()){
                    val current = productsDuplicate[p].asJsonObject
                    if (current.getAsJsonPrimitive("productId").asString == product.id){
                        exist = true
                        current.addProperty("count", current.get("count").asInt + count)
                        new.keySet().forEach{
                            if (current.get(it) == null){
                                ProductActions().setToJson(current, JsonPrimitive(it).asString, new.getAsJsonPrimitive(it).asString)
                            }
                        }
                        productsDuplicate[p] = current
                        break
                    }
                }
                if (!exist) {productsDuplicate.add(new)}

                Favourite.add("products", productsDuplicate)
            }

            val noStat = if(Favourite.getAsJsonArray("products") != null){
                Favourite.getAsJsonArray("products")
            }else{
                JsonArray()
            }
            retrofit.addToFavourite(getClientName(), getSession(), JsonObject().apply {
                add("wishlist", JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", noStat)
                    })
            }).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun UpdateContacts(ctx: Context, e: String, p: String){
            val params = hashMapOf<String, String>()
            if (e.isNotEmpty()) {params.put("email", e)}
            if (p.isNotEmpty()) {params.put("phone", p)}

            if (loggedIn && ((email != e) || (phone != p))) {
                retrofit.updateContacts(
                    getClientName(),
                    getSession(),
                    params
                ).enqueue(object : Callback<Unit>{
                    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                        setEmail(ctx, e)
                        setPhone(ctx, p)
                        //onUpdateContactsCallback("New email: ${e}, new phone: ${p}")
                    }

                    override fun onFailure(call: Call<Unit>, t: Throwable) {
                        logInfo("error when updating")
                        //onUpdateContactsCallback("Error when updating: ${t.localizedMessage}")
                        onErrorCallback("Error when updating: ${t.localizedMessage}")
                    }
                })
                return
            }
        }

        private fun subscribeNew(ctx: Context, req: JsonObject){
            retrofit.subscribe(
                getClientName(),
                sessionId!!,
                req
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "success"
                    logInfo(msg)
                    setLoggedIn(ctx)
                    var eml = ""
                    var ph = ""
                    if (req.has("fields")){
                        val f = req.get("fields").asJsonObject
                        if (f.has("email")){
                            eml = f.get("email").asString
                        }
                        if (f.has("phone")){
                            ph = f.get("phone").asString
                        }
                    }
//                    setEmail(ctx, eml)
//                    setPhone(ctx, ph)
//                    setLoggedIn(ctx)
//                    loggedIn = true
                    onSubscriberCallback(msg)
                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when subscribing: ${t.localizedMessage}"
                    logInfo(msg)
                    //onSubscriberCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun AddExtraFields(fields: Map<String, Any>){
            if (fields.isEmpty()){
                logInfo("extrafields is empty")
                return
            }

            val extrafields = JsonObject()

            for (ef in fields){
                setToJson(extrafields, ef)
            }
            retrofit.addExtrafields(
                getClientName(),
                getSession(),
                JsonObject().apply { add("extrafields", extrafields) }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "extrafields added"
                    logInfo(msg)
                    onSubscriberCallback(msg)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding extrafields: ${t.localizedMessage}"
                    logInfo(msg)
                    onSubscriberCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun PageOpen(url: String){
            if (url.isEmpty()){
                return
            }
            retrofit.pageOpen(
                getClientName(),
                getSession(),
                PageUrl(url)
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "page opened"
                    logInfo(msg)
                    onProductActionCallback
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when saving page open: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }
    }

    class ProductActions {
        fun setToJson(obj: JsonObject, key:String, value: Any){
            when (value) {
                is Int -> obj.addProperty(key, value.toString().toInt())
                is Boolean -> obj.addProperty(key, value.toString().toBoolean())
                is Float -> obj.addProperty(key, value.toString().toFloat())
                else -> obj.addProperty(key, value.toString())
            }
        }

        fun setValue(obj: JsonObject, m: Map.Entry<String, Any>){
                setToJson(obj, m.key, m.value)
        }

        fun AddToCart(product:Product){
            if (product.id.isNullOrEmpty()) { logInfo("productId is empty"); return }

            val new = JsonObject().apply { addProperty("productId", product.id) }

            if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
            val count = if(product.count == 0) { 1 } else { product.count!! }
            new.addProperty("count", count)
            for (ef in product.fields.orEmpty()){
                setValue(new, ef)
            }

            val products = if(Cart.getAsJsonArray("products") != null){
                Cart.getAsJsonArray("products")
            }else{
                JsonArray()
            }

            var exist = false
            val productsDuplicate = products.deepCopy()
            for(p in 0 until products.count()){
                val current = productsDuplicate[p].asJsonObject
                if (current.getAsJsonPrimitive("productId").asString == product.id){
                    exist = true
                    current.addProperty("count", current.get("count").asInt + count)
                    new.keySet().forEach{
                        if (current.get(it) == null){
                            setToJson(current, JsonPrimitive(it).asString, new.getAsJsonPrimitive(it).asString)
                        }
                    }
                    productsDuplicate[p] = current
                    break
                }
            }
            if (!exist) {productsDuplicate.add(new)}

            Cart.add("products", productsDuplicate)
            val newDuplicate = new.deepCopy().apply { addProperty("action", "productAdd") }

            retrofit.addToCart(
                getClientName(),
                getSession(),
                JsonObject().apply {
                    add("cart", JsonObject()
                        .apply {
                            addProperty("lastUpdate", System.currentTimeMillis())
                            add("products", productsDuplicate)
                    })
                    add("history", JsonArray().apply { add(newDuplicate) })
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun RemoveFromCart(product: Product){
            if (product.id.isEmpty()) { logInfo("productId is empty"); return }

            val count = if(product.count == 0) { 1 } else { product.count!! }

            val products = if(Cart.getAsJsonArray("products") != null){
                Cart.getAsJsonArray("products")
            }else{
                logInfo("cart is empty")
                return
            }

            val productsDuplicate = products.deepCopy()
            for(p in 0 until products.count()){
                val current = productsDuplicate[p].asJsonObject
                if (current.getAsJsonPrimitive("productId").asString == product.id){
                    if (current.get("count").asInt - count <= 0){
                        productsDuplicate.remove(p)
                        break
                    } else {
                        current.addProperty("count", current.get("count").asInt - count)
                        productsDuplicate[p] = current
                        break
                    }

                }
            }
            if(productsDuplicate == products) { return }

            Cart.add("products", productsDuplicate)

            val new = JsonObject()
            new.addProperty("productId", product.id)
            if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
            new.addProperty("count", count)
            for (ef in product.fields.orEmpty()){
                setValue(new, ef)
            }
            new.addProperty("action", "productRemove")

            retrofit.removeFromCart(
                getClientName(),
                sessionId!!,
                JsonObject().apply {
                    add("cart", JsonObject()
                        .apply {
                            addProperty("lastUpdate", System.currentTimeMillis())
                            add("products", productsDuplicate)
                        })
                    add("history", JsonArray().apply { add(new) })
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun AddToFavourite(product: Product){
            if (product.id.isEmpty()) { logInfo("productId is empty"); return }

            val new = JsonObject().apply { addProperty("productId", product.id) }

            if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
            val count = if(product.count == 0) { 1 } else { product.count!! }
            new.addProperty("count", count)
            for (ef in product.fields.orEmpty()){
                setValue(new, ef)
            }

            val products = if(Favourite.getAsJsonArray("products") != null){
                Favourite.getAsJsonArray("products")
            }else{
                JsonArray()
            }

            var exist = false
            val productsDuplicate = products.deepCopy()
            for(p in 0 until products.count()){
                val current = productsDuplicate[p].asJsonObject
                if (current.getAsJsonPrimitive("productId").asString == product.id){
                    exist = true
                    current.addProperty("count", current.get("count").asInt + count)
                    new.keySet().forEach{
                        if (current.get(it) == null){
                            setToJson(current, JsonPrimitive(it).asString, new.getAsJsonPrimitive(it).asString)
                        }
                    }
                    productsDuplicate[p] = current
                    break
                }
            }
            if (!exist) {productsDuplicate.add(new)}

            Favourite.add("products", productsDuplicate)
            val newDuplicate = new.deepCopy().apply { addProperty("action", "productLike") }

            retrofit.addToFavourite(
                getClientName(),
                sessionId!!,
                JsonObject().apply {
                    add("wishlist", JsonObject()
                        .apply {
                            addProperty("lastUpdate", System.currentTimeMillis())
                            add("products", productsDuplicate)
                        })
                    add("history", JsonArray().apply { add(newDuplicate) })
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun RemoveFromFavourite(product: Product){
            if (product.id.isEmpty()) { logInfo("productId is empty"); return }

            val count = if(product.count == 0) { 1 } else { product.count!! }

            val products = if(Favourite.getAsJsonArray("products") != null){
                Favourite.getAsJsonArray("products")
            }else{
                logInfo("cart is empty")
                return
            }

            val productsDuplicate = products.deepCopy()
            for(p in 0 until products.count()){
                val current = productsDuplicate[p].asJsonObject
                if (current.getAsJsonPrimitive("productId").asString == product.id){
                    if (current.get("count").asInt - count <= 0){
                        productsDuplicate.remove(p)
                        break
                    } else {
                        current.addProperty("count", current.get("count").asInt - count)
                        productsDuplicate[p] = current
                        break
                    }

                }
            }
            if(productsDuplicate == products) { return }
            Favourite.add("products", productsDuplicate)

            val new = JsonObject()
            new.addProperty("productId", product.id)
            if(!product.groupId.isNullOrEmpty()) { new.addProperty("groupId", product.groupId) }
            new.addProperty("count", count)
            for (ef in product.fields.orEmpty()){
                setValue(new, ef)
            }
            new.addProperty("action", "productDislike")

            retrofit.removeFromFavourite(
                getClientName(),
                sessionId!!,
                JsonObject().apply {
                    add("wishlist", JsonObject()
                        .apply {
                            addProperty("lastUpdate", System.currentTimeMillis())
                            add("products", productsDuplicate)
                        })
                    add("history", JsonArray().apply { add(new) })
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun ProductOpen(product: Product){
            if (product.id.isEmpty()){
                logInfo("productId is empty")
                return
            }

            val params = JsonObject().apply {
                if(product.groupId.isNullOrEmpty()) { addProperty("groupId", product.groupId) }
                for (ef in product.fields.orEmpty()){
                    setValue(this, ef)
                }
            }

            val productRequest = JsonObject().apply {
                addProperty("id", product.id)
                add("params", params)
            }

            retrofit.productOpen(
                getClientName(),
                sessionId!!,
                JsonObject().apply {
                    addProperty("action", "productOpen")
                    add("product", productRequest)
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "product opened"
                    logInfo(msg)
                    onProductActionCallback(msg)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when saving product open: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun ProductBuy(order: Order){
            if (order.id.isNullOrEmpty()){
                order.id = UUID.randomUUID().toString()
            }

            val orderInfo = JsonObject()
            val items = if(Cart.getAsJsonArray("products") != null){
                Cart.getAsJsonArray("products")
            }else{
                logInfo("cart is empty")
                return
            }

            orderInfo.add("items", items)
            orderInfo.add("order", JsonObject().apply {
                if (order.sum != null){ addProperty("sum",order.sum)}
                if (order.price != null){ addProperty("price", order.price) }

                order.fields?.forEach {
                    setToJson(this, it.key, it.value)
                }
            })

            retrofit.order(
                getClientName(),
                sessionId!!,
                JsonObject().apply {
                    addProperty("orderId", order.id)
                    add("orderInfo", orderInfo)
                }
            ).enqueue(object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "buying ok"
                    logInfo(msg)
                    ClearCart()
                    onProductActionCallback(msg)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when buying: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }

        fun ClearCart(){
            Cart = JsonObject()
            retrofit.addToCart(getClientName(), getSession(), JsonObject()
                .apply {
                add("cart", JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", JsonObject())
                    })
            }).enqueue(object : Callback<Unit>{
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    val msg = "success"
                    logInfo(msg)
                    onProductActionCallback(msg)

                }
                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when adding product to cart: ${t.localizedMessage}"
                    logInfo(msg)
                    onProductActionCallback(msg)
                    onErrorCallback(msg)
                }
            })
        }
    }
}


