package iot.example.devices.server

import android.provider.SyncStateContract
import iot.example.devices.HardwareException
import iot.example.devices.PrinterException

class ApiException(override val message: String,
                   val code: Int,
                   val deviceId: String,
                   val recoverable: Boolean,
                   val data: Any?,
                   override val cause: Throwable? = null) : RuntimeException() {
    companion object {
        fun fromHardwareException(hardwareException: HardwareException) =
                hardwareException.run {
                    ApiException(message = this.message ?: "Неизвестная ошибка",
                            code = this.code,
                            deviceId = this.deviceId,
                            recoverable = this.recoverable,
                            data = (this as? PrinterException)?.data,
                            cause = this)
                }

        fun HardwareException.toApiException() =
                fromHardwareException(this)

        //DeviceID такого вида из-за легаси
        fun notImplemented(message: String, deviceId: String = "????:????") =
                ApiException(message = message, code = SyncStateContract.Constants.ERROR_NOT_IMPLEMENTED,
                        deviceId = deviceId, recoverable = true, data = null)


        fun deviceNotFound(message: String) =
                ApiException(message, SyncStateContract.Constants.ERROR_NO_DEVICE, "????:????", true, null, null)

        fun fromUnexpectedException(unexpectedException: Throwable,
                                    deviceId: String?) =
                unexpectedException.run {
                    ApiException(message = this.message ?: "Неизвестная ошибка",
                            code = SyncStateContract.Constants.ERROR_UNKNOWN,
                            deviceId = deviceId ?: "????:????",
                            recoverable = true,
                            data = null,
                            cause = this)
                }
    }
}