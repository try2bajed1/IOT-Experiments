package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import iot.example.devices.Revision
import org.http4k.core.*
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.v2.ServerGson.auto


class DeviceInfoHandler(kodein: Kodein) : HttpHandler {

    private val holder: DeviceHolder = kodein.instance()
    private val injector = Body.auto<BoxInfoResponse>().toLens()

    override fun invoke(req: Request): Response {
        val info = BoxInfoResponse(
                meta = ResponseMeta(),
                info = BoxInfo(
                        version = Revision.VERSION,
                        deviceInfo = holder.getDevicesState()
                )
        )
        return injector.inject(info, Response(Status.OK))
    }
}