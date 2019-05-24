package iot.example.devices.v2

import org.http4k.core.*
import iot.example.devices.v2.ServerGson.auto
import java.util.logging.Logger.getLogger

object V2ErrorFilter : Filter {
    private val injector = Body.auto<DeviceResponse<DeviceInfo, Unit>>().toLens()

    override fun invoke(next: HttpHandler): HttpHandler = { req ->
        try {
            next(req)
        } catch (t: Throwable) {
            when (t) {
                is NotFoundException -> {
                    val resp = DeviceResponse<DeviceInfo, Unit>(
                            meta = ResponseMeta(t.code, t.message),
                            info = null,
                            data = null)
                    injector.inject(resp, Response(Status(555,"device not found")))
                }
                is ApiHardwareException -> {
                    val resp = DeviceResponse(
                            meta = ResponseMeta(t.code, t.message),
                            info = t.deviceInfo,
                            data = null)
                    injector.inject(resp, Response(Status.OK))
                }
                else -> {
                    logger.errToSenrty("Unknown error occurred.", t)
                    val resp = DeviceResponse(
                            meta = ResponseMeta(ERROR_UNKNOWN, "Неизвестная ошибка. Описание: ${t.message}"),
                            info = null,
                            data = null)
                    injector.inject(resp, Response(Status.INTERNAL_SERVER_ERROR))
                }
            }

        }


    }

    private val logger = getLogger()

}