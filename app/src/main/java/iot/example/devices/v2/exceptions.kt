package iot.example.devices.v2

import android.provider.SyncStateContract


class NotFoundException(override val message: String,
                        val code: Int = SyncStateContract.Constants.ERROR_NO_DEVICE) : RuntimeException()

class ApiHardwareException(override val message: String,
                           val code: Int,
                           val deviceInfo: DeviceInfo?) : RuntimeException()