package iot.example.devices

import android.content.Context
import android.hardware.usb.UsbDevice
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.withClass
import org.slf4j.Logger
import org.usb4java.*
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import javax.usb.UsbDevice
import javax.usb.UsbHostManager
import javax.usb.UsbHub
import kotlin.concurrent.thread
import org.usb4java.Device as LowLevelDevice


class UsbDevicesScanner(override val kodein: Kodein) : KodeinAware {

    private val logger: Logger = withClass().instance()
    @Volatile
    private var abort: Boolean = false

    fun abort() {
        this.abort = true
    }

    fun startScanUsbDevices() {
        //Запуск потока обработки новых USB событий.
        thread {
            // Initialize the libusb context
            var result = LibUsb.init(null)
            if (result != LibUsb.SUCCESS) {
                throw LibUsbException("Unable to initialize libusb", result)
            }

            // Check if hotplug is available
            if (!LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG)) {
                logger.error("libusb doesn't support hotplug on this system")
                return@thread
            }

            val callback: HotplugCallback = UsbCallback(instance())

            val callbackHandle = HotplugCallbackHandle()
            result = LibUsb.hotplugRegisterCallback(null,
                    LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED or LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
                    LibUsb.HOTPLUG_ENUMERATE,
                    LibUsb.HOTPLUG_MATCH_ANY,
                    LibUsb.HOTPLUG_MATCH_ANY,
                    LibUsb.HOTPLUG_MATCH_ANY,
                    callback, null, callbackHandle)

            if (result != LibUsb.SUCCESS) {
                throw LibUsbException("Unable to register hotplug callback", result)
            }

            while (!this.abort) {
                // Let libusb handle pending events. This blocks until events
                // have been handled, a hotplug callback has been deregistered
                // or the specified time of 1 second (Specified in Microseconds) has passed.
                val handleResult = LibUsb.handleEventsTimeout(null, 100000)
                if (handleResult != LibUsb.SUCCESS)
                    logger.error("Can't handle event", LibUsbException("Unable to handle events", result))
            }
        }
    }

    /**
     * The hotplug callback handler
     */
    class UsbCallback(private val deviceManager: DeviceManager) : HotplugCallback {

        override fun processEvent(context: Context, device: LowLevelDevice, event: Int, userData: Any?): Int {
            try {
                val descriptor = DeviceDescriptor()
                val result = LibUsb.getDeviceDescriptor(device, descriptor)
                if (result != LibUsb.SUCCESS)
                    throw LibUsbException("Unable to read device descriptor", result)
                //Костыль, защита от кривого устройства
                val vid = Util.unsignedShort(descriptor.idVendor())
                val pid = Util.unsignedShort(descriptor.idProduct())
                logger.info((if (event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED) "Connected" else "Disconnected") + " " + createVidPidKey(vid, pid))
                when (event) {
                    LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED -> {
                        //Устройства на высокий уровень приезжают не сразу. Возможности синхронизироваться нет.
                        Thread.sleep(1000)
                        val highLevelDevice = getHighLevelDevice(vid, pid) ?: return 0
                        if (deviceManager.newUsbDeviceAttached(vid, pid, highLevelDevice))
                            return 0
                    }

                    LibUsb.HOTPLUG_EVENT_DEVICE_LEFT ->
                        if (deviceManager.usbDeviceDetached(vid, pid))
                            return 0
                }
            } catch (t: Throwable) {
                //Защита от остановки сканирования
                logger.error("Unexpected error.", t)
            }
            return 0
        }

        private fun getHighLevelDevice(vid: Int, pid: Int,
                                       hub: UsbHub = UsbHostManager.getUsbServices().rootUsbHub): UsbDevice? {
            @Suppress("UNCHECKED_CAST")
            for (device in hub.attachedUsbDevices as List<UsbDevice>) {
                val desc = device.usbDeviceDescriptor
                val newVid = Util.unsignedShort(desc.idVendor())
                val newPid = Util.unsignedShort(desc.idProduct())
                if (newVid == vid && newPid == pid) {
                    return device
                }
                if (device.isUsbHub) {
                    //TODO Попробовать избавиться от рекурсии
                    return getHighLevelDevice(vid, pid, device as UsbHub) ?: continue
                }
            }
            return null
        }

        private companion object {
            private val logger = getLogger()
        }
    }

}
