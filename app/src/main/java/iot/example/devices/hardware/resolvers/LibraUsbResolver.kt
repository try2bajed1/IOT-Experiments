package iot.example.devices.resolvers

import android.hardware.usb.UsbDevice
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.typesafe.config.Config
import iot.example.devices.WEIGHING_BUS_TAG
import iot.example.devices.device.LibraUsb
import javax.usb.UsbDevice

class LibraUsbResolver(private val injector: Kodein) : Resolver<LibraUsb> {
    override fun isAcceptable(vid: Int, pid: Int): Boolean =
            supportedIds.contains(createVidPidKey(vid, pid))


    override fun newDevice(vid: Int, pid: Int, device: UsbDevice): LibraUsb =
            LibraUsb(vid, pid, device, injector.instance(WEIGHING_BUS_TAG),
                    injector.instance<Config>().getDuration("libraUsb.refreshTime"))


    private companion object {
        val supportedIds = listOf<Pair<Int, Int>>(VID_MASSAK_10C4 to PID_MASSAK_EA60, VID_MASSAK to PID_MASSAK)
                .map { createVidPidKey(it.first, it.second) }
                .toSet()
    }
}