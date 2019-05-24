package iot.example.devices.server.pojo

import android.provider.SyncStateContract
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class BoxStateInfo(@Expose @SerializedName("version") val version: String,
                        @Expose @SerializedName("serialNumber") val serialNumber: String,
                        @Expose @SerializedName("devices") val deviceStates: List<DeviceStateInfo> = emptyList(),
                        @Expose @SerializedName("message") val message: String = "OK",
                        @Expose @SerializedName("code") val code: Int = SyncStateContract.Constants.ERROR_OK)

/**
 * Класс описания подключенного устройства.
 * @param type - внутрений код, идентифицирующий устройство
 * @param typeName - имя типа, необходимо для удобства тестирования API
 * @param deviceName - имя девайса, выводимое пользователю на планшете
 * @param hexPid - PID устройства
 * @param hexVid - VID устройства
 */
data class DeviceStateInfo(@Expose @SerializedName("type") val type: Int,
                           @Expose @SerializedName("typeName") val typeName: String,
                           @Expose @SerializedName("deviceName") val deviceName: String? = null,
                           @Expose @SerializedName("vid") val hexVid: String,
                           @Expose @SerializedName("pid") val hexPid: String,
                           @Expose @SerializedName("index") val index: String? = null)

