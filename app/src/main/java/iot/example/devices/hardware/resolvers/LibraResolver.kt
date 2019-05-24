package iot.example.devices.resolvers

import android.hardware.usb.UsbDevice
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.typesafe.config.Config
import iot.example.devices.WEIGHING_BUS_TAG
import iot.example.devices.device.Libra
import javax.usb.UsbDevice

class LibraResolver(private val injector: Kodein) : Resolver<Libra> {
    override fun isAcceptable(vid: Int, pid: Int): Boolean =
            supportedIds.contains(createVidPidKey(vid, pid))


    override fun newDevice(vid: Int, pid: Int, device: UsbDevice): Libra {
        return Libra(vid, pid, device, injector.instance(WEIGHING_BUS_TAG),
                injector.instance<Config>().getDuration("libra.refreshTime"))
    }

    private companion object {
        val supportedIds = listOf<Pair<Int, Int>>(
                VID_PL2303 to PID_PL2303,
                VID_HL340 to PID_HL340)
                .map { createVidPidKey(it.first, it.second) }
                .toSet()
    }
}