package iot.example.devices.server.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ScanningRequest(@Expose @SerializedName("rollbarAccountInfo") val rollbarAccountInfo: String,
                           @Expose @SerializedName("timeout") val timeout: Int)

data class ScanningData(@Expose @SerializedName("barcode") val barcode: String,
                        @Expose @SerializedName("deviceId") val deviceId: String,
                        @Expose @SerializedName("message") val message: String = "OK",
                        @Expose @SerializedName("code") val code: Int = ERROR_OK)