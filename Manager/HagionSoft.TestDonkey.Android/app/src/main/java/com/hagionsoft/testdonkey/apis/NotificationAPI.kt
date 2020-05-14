package com.hagionsoft.testdonkey.apis

import com.hagionsoft.testdonkey.models.Notification
import retrofit2.Call
import retrofit2.http.*

interface NotificationAPI {
    @Headers("X-API-Key: cxFvfZ7Sze9aQ1xEgTM0a4hWjE4Bm25w2UKXCNdc")
    @POST("notifications")
    fun createNotification(@Body notification: Notification?): Call<String?>?

    @Headers("X-API-Key: cxFvfZ7Sze9aQ1xEgTM0a4hWjE4Bm25w2UKXCNdc")
    @PUT("notifications")
    fun updateNotification(@Body notification: Notification?): Call<String?>?

    @Headers("X-API-Key: cxFvfZ7Sze9aQ1xEgTM0a4hWjE4Bm25w2UKXCNdc")
    @DELETE("notifications")
    fun deleteNotification(@Query("topic_id") topicId: String): Call<String?>?

    @Headers("X-API-Key: cxFvfZ7Sze9aQ1xEgTM0a4hWjE4Bm25w2UKXCNdc")
    @DELETE("notifications/{id}")
    fun removeNotification(@Path("id") topicId: String): Call<String?>?
}