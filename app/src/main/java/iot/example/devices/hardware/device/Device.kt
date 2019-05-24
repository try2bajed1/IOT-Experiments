package iot.example.devices.device

import android.hardware.usb.UsbDevice
import iot.example.devices.DeviceType
import javax.usb.UsbDevice


abstract class Device<T : Device<T>>(open val vid: Int,
                                     open val pid: Int,
                                     open val usbDevice: UsbDevice,
                                     open val deviceType: DeviceType<T>,
                                     open val deviceName: String) {
    abstract fun detached()

    val vidPidKey get() = createVidPidKey(vid, pid)
}