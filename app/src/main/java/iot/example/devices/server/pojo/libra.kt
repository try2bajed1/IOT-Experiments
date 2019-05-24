package iot.example.devices.server.pojo

import android.provider.SyncStateContract
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class WeighingData(@Expose @SerializedName("weight") val weight: Int,
                        @Expose @SerializedName("deviceId") val deviceId: String,
                        @Expose @SerializedName("message") val message: String = "OK",
                        @Expose @SerializedName("code") val code: Int = SyncStateContract.Constants.ERROR_OK)