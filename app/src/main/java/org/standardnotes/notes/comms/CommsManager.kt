package org.standardnotes.notes.comms

import com.google.gson.*
import org.standardnotes.notes.SApplication

import java.io.IOException

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

/**
 * Created by carl on 03/01/17.
 */

class CommsManager(serverBaseUrl: String) {

    private val retrofit: Retrofit
    private val okHttpClient: OkHttpClient
    val api: ServerApi

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BODY
        okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(logger)
                .addInterceptor { chain ->
                    val original = chain.request()

                    //                        if (SApplication.getInstance().getToken() != null) {
                    // Request customization: add request headers
                    val requestBuilder = original.newBuilder()
                    if (SApplication.instance!!.valueStore.token != null) {
                        requestBuilder.header("Authorization", "Bearer " + SApplication.instance!!.valueStore.token)
                    }

                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .build()
        retrofit = Retrofit.Builder()
                .baseUrl(serverBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(SApplication.instance!!.gson))
                .build()
        api = retrofit.create(ServerApi::class.java)
    }

    class DateTimeDeserializer : JsonDeserializer<DateTime>, JsonSerializer<DateTime> {

        @Throws(JsonParseException::class)
        override fun deserialize(je: JsonElement, type: Type,
                                 jdc: JsonDeserializationContext): DateTime? {
            if (je.asString.length == 0) {
                return null
            } else {
                var parsed: DateTime
                try {
                    parsed = DATE_TIME_FORMATTER.parseDateTime(je.asString)
                } catch (e: IllegalArgumentException) {
                    parsed = DATE_TIME_FORMATTER_NOMILLIS.parseDateTime(je.asString)
                }

                return parsed
            }
        }

        override fun serialize(src: DateTime?, typeOfSrc: Type,
                               context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(if (src == null) "" else DATE_TIME_FORMATTER.print(src))
        }

        companion object {
            val DATE_TIME_FORMATTER: org.joda.time.format.DateTimeFormatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)
            val DATE_TIME_FORMATTER_NOMILLIS: org.joda.time.format.DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC)
        }
    }
}
