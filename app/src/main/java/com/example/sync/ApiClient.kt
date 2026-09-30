package com.example.sync

import com.example.BuildConfig
import com.example.data.model.Delivery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed class ApiResult {
    data class Success(val serverId: String, val message: String, val isDuplicate: Boolean = false) : ApiResult()
    data class Error(val message: String, val statusCode: Int? = null) : ApiResult()
}

class ApiClient(
    private var baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    fun setBaseUrl(url: String) {
        this.baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    /**
    * Uploads a delivery to the configured backend.
     */
    suspend fun uploadDelivery(
        delivery: Delivery,
        overrideMode: NetworkOverrideMode
    ): ApiResult = withContext(Dispatchers.IO) {
        if (overrideMode == NetworkOverrideMode.SIMULATE_SERVER_500) {
            delay(900)
            return@withContext ApiResult.Error("HTTP 500: Internal Server Error (Simulated Site Outage)", 500)
        }

        // Check if live server is reachable
        val file = File(delivery.localPhotoUri)
        val photoExists = file.exists()

        try {
            val url = "$baseUrl/api/deliveries"
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("supplierName", delivery.supplierName)
                .addFormDataPart("poNumber", delivery.poNumber)
                .addFormDataPart("note", delivery.note)
                .addFormDataPart("idempotencyKey", delivery.idempotencyKey)

            if (photoExists) {
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                builder.addFormDataPart("photo", file.name, file.asRequestBody(mediaType))
            }

            val requestBody = builder.build()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val serverId = json.optString("id", "srv_${System.currentTimeMillis()}")
                val isDuplicate = json.optBoolean("isDuplicate", false)
                return@withContext ApiResult.Success(
                    serverId = serverId,
                    message = if (isDuplicate) "Processed (Existing Idempotent Record)" else "Synced to PostgreSQL",
                    isDuplicate = isDuplicate
                )
            } else {
                return@withContext ApiResult.Error("Server returned code ${response.code}: $responseBody", response.code)
            }
        } catch (e: Exception) {
            return@withContext ApiResult.Error(
                "Unable to reach delivery API: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }
}
