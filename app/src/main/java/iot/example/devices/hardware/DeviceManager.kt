package iot.example.devices

import android.hardware.usb.UsbDevice
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.instance
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.resolvers.Resolver
import javax.usb.UsbDevice

/**
 * Класс - контроллер девайсов.
 * Обрабатывает подключение/отключение USB устройств, и передаёт управление контроллерам специфичных устройств.
 */
class DeviceManager(override val kodein: Kodein) : KodeinAware {
    private val deviceHolder: DeviceHolder = instance()
    private val resolvers: Set<Resolver<*>> = instance()

    fun newUsbDeviceAttached(vid: Int, pid: Int, device: UsbDevice): Boolean {
        val resolver = resolvers.firstOrNull { it.isAcceptable(vid, pid) }
        if (resolver == null) {
            unknownDevice(vid, pid, device)
                return false
        }
        try {
            resolver
                    .newDevice(vid = vid, pid = pid, device = device)
                    .also {
                        logger.info("New device with type ${it.deviceType} connected.")
                        deviceHolder.removeDevice(it.vid, it.pid)?.let {
                            logger.warn("Old device with same type was disconnected. " +
                                    "Only 1 device of each type supported currently.")
                            it.detached()
                        }
                        deviceHolder.addDevice(it)
                    }
            return true
        } catch (h: HardwareException) {
            logger.error("Error, while init new device", h)
        }
        return false
    }

    fun usbDeviceDetached(vid: Int, pid: Int): Boolean =
            deviceHolder.removeDevice(vid, pid)
                    ?.let {
                        it.detached()
                        logger.info("Device of type ${it.deviceType} was detached.")
                        true
                    } ?: false

    private fun unknownDevice(vid: Int, pid: Int, device: UsbDevice) {
        logger.info("Unknown device: $vid:$pid ${device.usbDeviceDescriptor}")

    }

}