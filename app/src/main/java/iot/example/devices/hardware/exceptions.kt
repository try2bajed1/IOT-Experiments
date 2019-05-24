package iot.example.devices

import android.provider.SyncStateContract

class ScannerException(message: String?,
                       deviceId: String,
                       code: Int = SyncStateContract.Constants.ERROR_UNKNOWN,
                       recoverable: Boolean = true,
                       cause: Throwable? = null) :
        HardwareException(message, code, deviceId = deviceId, recoverable = recoverable, cause = cause)

class LibraException(message: String?,
                     code: Int = SyncStateContract.Constants.ERROR_UNKNOWN,
                     deviceId: String,
                     recoverable: Boolean = true,
                     cause: Throwable? = null) :
        HardwareException(message, code, deviceId = deviceId, recoverable = recoverable, cause = cause)

class LibraUsbException(message: String?,
                        code: Int = SyncStateContract.Constants.ERROR_UNKNOWN,
                        deviceId: String,
                        recoverable: Boolean = true,
                        cause: Throwable? = null) :
        HardwareException(message, code, deviceId = deviceId, recoverable = recoverable, cause = cause)

class SerialException(message: String?,
                      code: Int = SyncStateContract.Constants.ERROR_UNKNOWN,
                      recoverable: Boolean = true,
                      cause: Throwable? = null) :
        HardwareException(message, code, deviceId = "????:????", recoverable = recoverable, cause = cause)


open class HardwareException(override val message: String?,
                             val code: Int,
                             val deviceId: String,
                             val recoverable: Boolean = true,
                             override val cause: Throwable? = null) : RuntimeException()