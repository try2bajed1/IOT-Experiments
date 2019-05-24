package iot.example.devices.server

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.typesafe.config.Config
import io.reactivex.Observable
import org.http4k.core.*
import iot.example.devices.WEIGHING_BUS_TAG
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.DeviceType
import iot.example.devices.LibraException
import iot.example.devices.server.ServerGson.auto
import iot.example.devices.server.pojo.WeighingData
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LibraHandler(kodein: Kodein) : HttpHandler {

    private val deviceHolder: DeviceHolder = kodein.instance()
    private val responseLens = Body.auto<WeighingData>().toLens()
    private val weighingBus: Observable<BigDecimal> = kodein.instance(WEIGHING_BUS_TAG)
    private val timeout = kodein.instance<Config>()
            .getDuration("libra.refreshTime")
            .multipliedBy(2)

    override fun invoke(req: Request): Response {
        val vidPid = deviceHolder.useDevice(DeviceType.LIBRA) { it.vidPidKey }
                ?: throw ApiException.deviceNotFound("Весы не подключены")
        try {
            val result = weighingBus
                    .distinctUntilChanged { f, s -> f.compareTo(s) == 0 }
                    .skip(1)
                    .timeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .blockingFirst()

            val resp = WeighingData((result * BigDecimal(1000)).intValueExact(), vidPid)

            return responseLens.inject(resp, Response(Status.OK))

        } catch (e: Throwable) {
            when (e) {
                is TimeoutException -> {
                    //Девайс отвалился
                    deviceHolder.removeDevice(DeviceType.LIBRA)
                    throw ApiException.fromHardwareException(
                            LibraException("Не получается считать данные от весов.", deviceId = vidPid)
                    )
                }
                else -> throw ApiException.fromUnexpectedException(e, vidPid)
            }
        }
    }
}