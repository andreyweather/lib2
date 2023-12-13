package com.enkod.enkodpushlibrary

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import com.example.jetpack_new.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit


object EnkodPushLibrary {

    private const val TAG = "EnkodPushLibrary"
    private const val SESSION_ID_TAG: String = "${TAG}_SESSION_ID"
    private const val TOKEN_TAG: String = "${TAG}_TOKEN"
    private const val ACCOUNT_TAG: String = "${TAG}_ACCOUNT"

    internal val CHANEL_Id = "enkod_lib_1"
    internal val notificationId = 1



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
    private lateinit var email: String
    private lateinit var phone: String

    internal val vibrationPattern = longArrayOf(1500, 500)
    internal val defaultIconId: Int = R.drawable.ic_android_black_24dp
    internal var intentName = "intent"
    internal var token: String? = null
    internal var sessionId: String? = null
    private var onPushClickCallback: (Bundle, String) -> Unit = { _, _ -> }
    private var onDynamicLinkClick: ((String) -> Unit)? = null
    private var newTokenCallback: (String) -> Unit = {}
    private var onDeletedMessage: () -> Unit = {}
    private var onProductActionCallback: (String) -> Unit = {}
    private var onErrorCallback: (String) -> Unit = {}

    private lateinit var retrofit: Api
    private lateinit var client: OkHttpClient

    // класс необходим для правильной работы библиотеки retrofit

    class NullOnEmptyConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            Log.d("Library", "responseBodyConverter")
            val delegate: Converter<ResponseBody, *> =
                retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter { body ->
                if (body.contentLength() == 0L) null else delegate.convert(
                    body
                )
            }
        }
    }

    // класс перечислений (OpenIntent) необходим для обработки нажатия на push

    enum class OpenIntent {
        DYNAMIC_LINK, OPEN_URL, OPEN_APP;
        fun get(): String {
            Log.d("Library", "get")
            return when (this) {
                DYNAMIC_LINK -> "0"
                OPEN_URL -> "1"
                OPEN_APP -> "2"
            }
        }

        companion object {
            fun get(intent: String?): OpenIntent {
                Log.d("Library", "get")
                return when (intent) {
                    "0" -> DYNAMIC_LINK
                    "1" -> OPEN_URL
                    "2" -> OPEN_APP
                    else -> OPEN_APP
                }
            }
        }
    }

    // данная функция (init) производит инициализацию библиотеки - запускает необходимые функции

    @SuppressLint("SuspiciousIndentation")
    internal fun init(ctx: Context, account: String, _email: String?, _phone: String?, firebaseIndicator: Int) {

        initRetrofit()
        setClientName(ctx, account)

        var preferencesSessionId: String? = ""
        var preferencesToken: String? = ""


        if (_email != null) {
            email= _email
        }else email = ""

        if (_phone != null) {
            phone = _phone
        }else phone = ""

        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        preferencesSessionId = preferences.getString(SESSION_ID_TAG, null)
        preferencesToken = preferences.getString(TOKEN_TAG, null)


        this.sessionId = preferencesSessionId
        this.token = preferencesToken

        Log.d ("session_token", "$sessionId, $token")

        if (firebaseIndicator == 0 && preferencesSessionId.isNullOrEmpty()) {
            getSessionIdFromApi(ctx)
        }

        if (!this.sessionId.isNullOrEmpty()) {

            startSession()

        }
    }

    // функция (initRetrofit) инициализации библиотеки retrofit - выполняющую http запросы

     fun initRetrofit() {
        Log.d("Library", "initRetrofit")
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

        var baseUrl = "http://dev.ext.enkod.ru/"

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(Api::class.java)
    }

    /* данная функция (getToken) получает информацию о текущем токене. Если текущей токен не равен
       сохраненному токену, происходит запись значения нового токена.

       также в данной функции выполняет проверка сохраненного значения sessionId.
       Если значение = null запускается функция получение новой сессии
    */

    internal fun getToken(ctx: Context, token: String?) {

        Log.d ("Library", "getToken")

        var preferencesSessionId: String? = ""
        var preferencesToken: String? = ""

        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        preferencesSessionId = preferences.getString(SESSION_ID_TAG, null)
        preferencesToken = preferences.getString(SESSION_ID_TAG, null)

        this.sessionId = preferencesSessionId
        this.token = preferencesToken


         if (this.token != token) {

            val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            preferences.edit()
                .putString(TOKEN_TAG, token)
                .apply()
            this.token = token

            Log.d("new_token", this.token.toString())
        }

        if (preferencesSessionId.isNullOrEmpty()) {
            getSessionIdFromApi(ctx)
        }


    }

    /* функция (getSessionIdFromApi) получения новой сессии (session_Id)
       запускает функцию (newSessions) сохранения новой сессии.
   */

    private fun getSessionIdFromApi(ctx: Context) {

        Log.d ("Library", "getSessionIdFromApi")

        retrofit.getSessionId(getClientName()).enqueue(object : Callback<SessionIdResponse> {
            override fun onResponse(
                call: Call<SessionIdResponse>,
                response: Response<SessionIdResponse>
            ) {
                response.body()?.session_id?.let {
                    logInfo("get token from api $it\n")
                    Log.d("new_session", it)
                    newSessions(ctx, it)
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

    /* функция (newSessions) сохранения  новой сессии (session_Id)
      запускает функцию (updateToken) которая создает запись о текущей сессии и токене на сервисе.
    */

    private fun newSessions(ctx: Context, nsession: String?) {

        Log.d("Library", "newSessions")


        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        var newPreferencesToken = preferences.getString(TOKEN_TAG, null)

        preferences.edit()
            .putString(SESSION_ID_TAG, nsession)
            .apply()
        this.sessionId = nsession

        Log.d("session", this.sessionId.toString())
        Log.d("new_token", newPreferencesToken.toString())

        if (newPreferencesToken.isNullOrEmpty()) {
            subscribeToPush {}
        } else updateToken(ctx, nsession, newPreferencesToken)

    }

    /* функция (updateToken) создает запись о текущей сессии и токене на сервисе
     запускает функцию (subscribeToPush) которая подключает контакт к push уведомлениям.
    */

    private fun updateToken(ctx: Context, session: String?, token: String?) {

        Log.d("updateToken", "updateToken")
        retrofit.updateToken(
            getClientName(),
            getSession(),
            SubscribeBody(
                sessionId = session!!,
                token = token!!
            )
        ).enqueue(object : Callback<UpdateTokenResponse> {
            override fun onResponse(
                call: Call<UpdateTokenResponse>,
                response: Response<UpdateTokenResponse>
            ) {
                logInfo("token updated")
                newTokenCallback(token!!)
                //subscribeToPush {}
                startSession()
            }

            override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                logInfo("token update failure")
            }

        })
    }

    private fun startSession() {
        var tokenSession = ""
        if (!this.token.isNullOrEmpty()) {
            tokenSession = this.token!!
        }
        Log.d ("start_session", "yes ${this.token}")
        tokenSession?.let {
            logInfo("on start session \n")
            sessionId?.let { it1 ->
                retrofit.startSession(it1, getClientName()).enqueue(object : Callback<SessionIdResponse> {
                    override fun onResponse(
                        call: Call<SessionIdResponse>,
                        response: Response<SessionIdResponse>
                    ) {
                        logInfo("session started ${response.body()?.session_id}")
                        //isSessionStarted = true
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



    //  функция (subscribeToPush) подключает контакт к push уведомлениям.

    private fun subscribeToPush(callback: (String) -> Unit) {

        var t = ""
        var token: String? = if (this.token != null) this.token else t

        Log.d("subscribeToPush", "subscribeToPush")
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
                //addContact(email, phone)
            }

            override fun onFailure(call: Call<UpdateTokenResponse>, t: Throwable) {
                logInfo("MESSAGE ${t.localizedMessage}")
                callback("failure")
            }

        })
    }


    // функция (addContact) создания и добавления нового контакта на сервис

        fun addContact(email: String, extrafileds: Map<String,String>? = null ) {

            Log.d("", "addContact")

            val req = JsonObject()

            req.add("mainChannel", Gson().toJsonTree("email"))

            val fileds = JsonObject()


            if (!extrafileds.isNullOrEmpty()) {
                val keys = extrafileds.keys

                for (i in 0 until keys.size) {

                    fileds.addProperty(keys.elementAt(i),extrafileds.getValue(keys.elementAt(i)))
                }
            }
            fileds.addProperty("email", email)

            req.add("fields", fileds)

            retrofit.subscribe(
                getClientName(),
                sessionId!!,
                req

            ).enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    val msg = "ok"
                    Log.d("succes", msg)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    val msg = "error when subscribing: ${t.localizedMessage}"
                    Log.d("error", msg)
                    //onSubscriberCallback(msg)
                    onErrorCallback(msg)
                }
            })
    }



    // функция (getClientName) возвращает имя клиента Enkod

    private fun getClientName(): String {
        Log.d("Library", "getClientName ${this.account.toString()}")
        return this.account
    }

    // функция (getSession) private возвращает значение сессии

    private fun getSession(): String {

        return if (!this.sessionId.isNullOrEmpty()) {
            Log.d("getSession", " $sessionId")
            this.sessionId!!
        } else {
            ""
        }
    }

    fun getSessionFromLibrary (context: Context): String {
        val preferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val preferencesSessionId = preferences.getString(SESSION_ID_TAG, null)
        Log.d("prefstring", preferencesSessionId.toString())
        return if (!preferencesSessionId.isNullOrEmpty()) {
            preferencesSessionId!!
        } else ""
    }

    fun getTokenFromLibrary(context: Context): String {
        val preferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        val preferencesToken = preferences.getString(TOKEN_TAG, null)
        return if (!preferencesToken.isNullOrEmpty()) {
            preferencesToken!!
        } else ""
    }




    // функция (logOut) уничтожения текущей сессии

    fun logOut(ctx: Context) {
        FirebaseMessaging.getInstance().deleteToken();
        val preferences = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit().remove(SESSION_ID_TAG).apply()
        preferences.edit().remove(TOKEN_TAG).apply()
        sessionId = ""
        token = ""
    }


    // функция (logInfo) создания тегов для отладки

    internal fun logInfo(msg: String) {
        Log.d("Library", "logInfo + ${msg}")
        Log.i(TAG, msg)
    }

    fun processMessage(context: Context, message: RemoteMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            createNotificationChannel(context)
            createNotification(context, message)
            Log.d("processMessage", message.data.toString())
        }

    }

    // функции (createNotificationChannel) и (createNotification) создают и показывают push уведомления

      fun createNotification(context: Context, message: RemoteMessage) {

        with(message.data) {

            val data = message.data
            Log.d ("message", data.toString())
            var url = ""

            if (data.containsKey("url") && data[url] != null) {
                url = data["url"].toString()
            }

            val builder = NotificationCompat.Builder(context, CHANEL_Id)


            val pendingIntent: PendingIntent =  getIntent(
                context, message.data, "", url
            )

            builder

                .setIcon(context, data["imageUrl"])
                .setColor(context, data["color"])
                .setLights(
                    get(ledColor), get(ledOnMs), get(
                        ledOffMs))
                .setVibrate(get(vibrationOn).toBoolean())
                .setSound(get(soundOn).toBoolean())
                .setContentTitle(data["title"])
                .setContentText(data["body"])
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addActions(context, message.data)
                .setPriority(NotificationCompat.PRIORITY_MAX)


           if (message.data["image"] != null) {
               if (getBitmapFromUrl(data["image"]) != null) {
                   try {
                       builder.setLargeIcon(getBitmapFromUrl(data["image"]))
                           .setLargeIcon(getBitmapFromUrl(data["image"]))
                           .setStyle(
                               NotificationCompat.BigPictureStyle()
                                   .bigPicture(getBitmapFromUrl(data["image"]))
                                   .bigLargeIcon(getBitmapFromUrl(data["image"]))

                           )
                   } catch (e: Exception) {
                       Log.d("error_in_set_img", e.toString())
                   }

               }

           }

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    return
                }

                  notify(message.data["messageId"]!!.toInt(), builder.build())

            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification Title"
            val descriptionText = "Notification Description"
            //val importance = NotificationManager.IMPORTANCE_MAX
            val channel = NotificationChannel(
                CHANEL_Id,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager? =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.createNotificationChannel(channel)
        }
    }

// функция (processMessage) запускает процесс создания push уведомлений

    internal fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        Log.d("Library", "getBitmapFromUrl")
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            try {
                connection.connect()
            } catch (e: Exception) {
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

    // функция (getIntent) определяет какой вид действия должен быть совершен после нажатия на push

    internal fun getIntent(
        context: Context,
        data: Map<String, String>,
        field: String,
        url: String
    ): PendingIntent {

        Log.d ("message_info", "${data["intent"].toString()} ${field.toString()}")
        val intent =
            if (field == "1") {
                getOpenUrlIntent(context, data, url)
            } else if (data["intent"] == "1") {
                getOpenUrlIntent(context, data, "null")
            } else if (field == "0") {
                getDynamicLinkIntent(context, data, url)
            } else if (data["intent"] == "0")
                getDynamicLinkIntent(context, data, "null")
            else {

                getOpenAppIntent(context)
            }

        intent!!.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
        Log.d("Library", "getOpenAppIntent")
        return Intent(context, OnOpenActivity::class.java).also {
            it.putExtras(
                bundleOf(
                    intentName to OpenIntent.OPEN_APP.get(),
                    OpenIntent.OPEN_APP.name to true
                )
            )
        }
    }

    // функция (getPackageLauncherIntent) создает намерение которое открывает приложение при нажатии на push
    internal fun getPackageLauncherIntent(context: Context): Intent? {

        Log.d("package_intent", "getPackageLauncherIntent")
        val pm: PackageManager = context.packageManager
        return pm.getLaunchIntentForPackage(context.packageName).also {
            val bundle = (
                bundleOf(
                    intentName to OpenIntent.OPEN_APP.get(),
                    OpenIntent.OPEN_APP.name to true
                )
            )
        }
    }

    /* функция (getDynamicLinkIntent) создает намерение которое открывает динамическую ссылку
       в приложении при нажатии на push (переход к определенной страннице приложения)
     */

    private fun getDynamicLinkIntent(
        context: Context,
        data: Map<String, String>,
        URL: String
    ): Intent {
        if (URL != "null") {
            return Intent(context, OnOpenActivity::class.java).also {
                it.putExtras(
                    bundleOf(
                        intentName to OpenIntent.DYNAMIC_LINK.get(),
                        OpenIntent.OPEN_APP.name to true,
                        this.url to URL
                    )
                )
            }

        } else {
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
    }

    /* функция (getOpenUrlIntent) создает намерение которое открывает внешнию http ссылку
      при нажатии на push (переход на сайт)
    */

    private fun getOpenUrlIntent(context: Context, data: Map<String, String>, URL: String): Intent {
        Log.d("Library", "getOpenUrlIntent")
        if (URL != "null") {
            return Intent(context, OnOpenActivity::class.java).also {
                it.putExtras(
                    bundleOf(
                        intentName to OpenIntent.OPEN_URL.get(),
                        OpenIntent.OPEN_APP.name to true,
                        this.url to URL
                    )
                )
            }
        } else {
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
    }


    // функция (setClientName) сохраняет значение имени клиента Enkod

    private fun setClientName(context: Context, acc: String) {
        Log.d("Library", "setClientName ${acc.toString()}")
        val preferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences
            .edit()
            .putString(ACCOUNT_TAG, acc)
            .apply()

        this.account = acc
    }

    /* функция (getResourceId) получает получить данные с сообщения передаваемого с сервиса
    для дальнейшего создания push
     */

    internal fun getResourceId(
        context: Context,
        pVariableName: String?,
        resName: String?,
        pPackageName: String?
    ): Int {
        Log.d("Library", "getResourceId")
        return try {
            context.resources.getIdentifier(pVariableName, resName, pPackageName)
        } catch (e: Exception) {
            e.printStackTrace()
            defaultIconId
        }
    }

    // функция (getBitmapFromUrl) требуется для открытия изображения в push уведомлении

    internal fun onDeletedMessage() {
        Log.d("Library", "onDeletedMessage")
        onDeletedMessage.invoke()
    }

    /* функции (set), (handleExtras), (sendPushClickInfo)  требуются для установки значений
    для отображения push уведомлений
     */

    internal fun set(hasVibration: Boolean): LongArray {
        Log.d("Library", "set_vibrationPattern")
        return if (hasVibration) {
            vibrationPattern
        } else {
            longArrayOf(0)
        }
    }

    fun handleExtras(context: Context, extras: Bundle) {
        val link = extras.getString(url)
        Log.d("handleExtras", "handleExtras ${extras.getString("messageId")}")
        sendPushClickInfo(extras)
        when (OpenIntent.get(extras.getString(intentName))) {
            OpenIntent.OPEN_URL -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(link))
                )
            }

            OpenIntent.DYNAMIC_LINK -> {
                link?.let {
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

    private fun sendPushClickInfo(extras: Bundle) {
        Log.d("sendPushClickInfo", "sendPushClickInfo")
        if (extras.getString(personId) != null && extras.getString(messageId) != null) {
            Log.d("sendPushClickInfo", "sendPushClickInfo_no_null")
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

    // функция (RemoveFromFavourite) фиксирует событые добавления в корзину
    fun AddToCart (product: Product) {

        if (!product.id.isNullOrEmpty()) {

            var req = JsonObject()
            val products = JsonObject()
            val history = JsonObject()
            var property = ""

            products.addProperty("productId", product.id)
            products.addProperty("count", product.count)

            history.addProperty("productId", product.id)
            history.addProperty("categoryId", product.categoryId)
            history.addProperty("count", product.count)
            history.addProperty("price", product.price)
            history.addProperty("picture", product.picture)



                    history.addProperty("action", "productAdd")
                    property = "cart"



            req = JsonObject().apply {
                add(property, JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", JsonArray().apply { add(products) })
                    })
                add("history", JsonArray().apply { add(history) })
            }

            Log.d("req", req.toString())


                retrofit.addToCart(
                    getClientName(),
                    sessionId!!,
                    req
                ).enqueue(object : Callback<Unit> {
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
        } else return
    }

    // функция (RemoveFromFavourite) фиксирует событые удаления из корзины
        fun RemoveFromCart (product: Product) {

        if (!product.id.isNullOrEmpty()) {

            var req = JsonObject()
            val products = JsonObject()
            val history = JsonObject()
            var property = ""

            products.addProperty("productId", product.id)
            products.addProperty("count", product.count)

            history.addProperty("productId", product.id)
            history.addProperty("categoryId", product.categoryId)
            history.addProperty("count", product.count)
            history.addProperty("price", product.price)
            history.addProperty("picture", product.picture)



            history.addProperty("action", "productRemove")
            property = "cart"


            req = JsonObject().apply {
                add(property, JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", JsonArray().apply { add(products) })
                    })
                add("history", JsonArray().apply { add(history) })
            }

            Log.d("req", req.toString())


            retrofit.addToCart(
                getClientName(),
                sessionId!!,
                req
            ).enqueue(object : Callback<Unit> {
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
        } else return
    }
    // функция (RemoveFromFavourite) фиксирует событые добавления из избранное
    fun AddToFavourite (product: Product) {

        if (!product.id.isNullOrEmpty()) {

            var req = JsonObject()
            val products = JsonObject()
            val history = JsonObject()
            var property = ""

            products.addProperty("productId", product.id)
            products.addProperty("count", product.count)

            history.addProperty("productId", product.id)
            history.addProperty("categoryId", product.categoryId)
            history.addProperty("count", product.count)
            history.addProperty("price", product.price)
            history.addProperty("picture", product.picture)


                    history.addProperty("action", "productLike")
                    property = "wishlist"



            req = JsonObject().apply {
                add(property, JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", JsonArray().apply { add(products) })
                    })
                add("history", JsonArray().apply { add(history) })
            }

            Log.d("req", req.toString())


                retrofit.addToFavourite(
                    getClientName(),
                    sessionId!!,
                    req
                ).enqueue(object : Callback<Unit> {
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

        } else return
    }

    // функция (RemoveFromFavourite) фиксирует событые удаления из избранного

    fun RemoveFromFavourite (product: Product) {

        if (!product.id.isNullOrEmpty()) {

            var req = JsonObject()
            val products = JsonObject()
            val history = JsonObject()
            var property = ""

            products.addProperty("productId", product.id)
            products.addProperty("count", product.count)

            history.addProperty("productId", product.id)
            history.addProperty("categoryId", product.categoryId)
            history.addProperty("count", product.count)
            history.addProperty("price", product.price)
            history.addProperty("picture", product.picture)


            history.addProperty("action", "productDislike")
            property = "wishlist"


            req = JsonObject().apply {
                add(property, JsonObject()
                    .apply {
                        addProperty("lastUpdate", System.currentTimeMillis())
                        add("products", JsonArray().apply { add(products) })
                    })
                add("history", JsonArray().apply { add(history) })
            }

            Log.d("req", req.toString())


            retrofit.addToFavourite(
                getClientName(),
                sessionId!!,
                req
            ).enqueue(object : Callback<Unit> {
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

        } else return
    }


    // функция (productBuy) для передачи информации о покупках на сервис

    fun productBuy(order: Order) {

        if (order.id.isNullOrEmpty()) {
            order.id = UUID.randomUUID().toString()
        }

        val orderInfo = JsonObject()
        val items = JsonArray()

        val position = JsonObject()
        position.addProperty("productId", order.productId)

        items.add(position)

        orderInfo.add("items", items)

        orderInfo.add("order", JsonObject().apply {
            if (order.sum != null) {
                addProperty("sum", order.sum)
            }
            if (order.price != null) {
                addProperty("price", order.price)
            }
            if (order.productId != null) {
                addProperty("productId", order.productId)
            }
            if (order.count != null) {
                addProperty("count", order.count)
            }
            if (order.picture != null) {
                addProperty("picture", order.picture)
            }

        })

        val req = JsonObject().apply {
            addProperty("orderId", order.id)
            add("orderInfo", orderInfo)
        }
        Log.d("buy", req.toString())
        retrofit.order(
            getClientName(),
            sessionId!!,
            req
        ).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                val msg = "buying ok"
                logInfo(msg)
                //ClearCart()
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

    // функция (ProductOpen) для передаци информации об открытии товаров на сервис

    fun ProductOpen(product: Product) {

        if (!product.id.isNullOrEmpty()) {

            val params = JsonObject()

            params.addProperty("categoryId", product.categoryId)
            params.addProperty("price", product.price)
            params.addProperty("picture", product.picture)


            val productRequest = JsonObject().apply {
                addProperty("id", product.id)
                add("params", params)
            }
            val req = JsonObject().apply {
                addProperty("action", "productOpen")
                add("product", productRequest)
            }

            Log.d("open", req.toString())

            retrofit.productOpen(
                getClientName(),
                sessionId!!,
                req

            ).enqueue(object : Callback<Unit> {
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
        } else return
    }
}





