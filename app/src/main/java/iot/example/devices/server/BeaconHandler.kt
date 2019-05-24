package iot.example.devices.server

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.instance
import iot.example.devices.Revision
import org.http4k.core.*
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.server.ServerGson.auto
import iot.example.devices.server.pojo.BoxStateInfo
import iot.example.devices.server.pojo.toDeviceStateInfo

class BeaconHandler(override val kodein: Kodein) : HttpHandler, KodeinAware {
    private val deviceHolder: DeviceHolder = kodein.instance()
    private val responseLens = Body.auto<BoxStateInfo>().toLens()

    override fun invoke(req: Request): Response =
            BoxStateInfo(Revision.VERSION,
                    "pi",
                    deviceHolder.getDevicesState().map { it.toDeviceStateInfo() })
                    .let {
                        responseLens.inject(it, Response(Status.OK))
                    }

}