package iot.example.devices.server.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Error(@Expose @SerializedName("code") val code: Int,
                 @Expose @SerializedName("deviceId") val deviceId: String,
                 @Expose @SerializedName("recoverable") val recoverable: Boolean,
                 @Expose @SerializedName("message") val message: String,
                 @Expose @SerializedName("retVal") val result: Any? = null,
                 @Expose @SerializedName("log") val log: List<String> = emptyList())
