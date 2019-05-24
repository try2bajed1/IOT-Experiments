package iot.example.devices.server

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Observable
import org.http4k.core.*
import iot.example.devices.WEIGHING_BUS_TAG
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.DeviceType
import iot.example.devices.server.ServerGson.auto
import iot.example.devices.server.pojo.UsbWeighingData
import iot.example.devices.server.pojo.UsbWeighingResult
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LibraUsbHandler(kodein: Kodein) : HttpHandler {
    private val deviceHolder: DeviceHolder = kodein.instance()
    private val weighingBus: Observable<BigDecimal> = kodein.instance(WEIGHING_BUS_TAG)
    private val weighingRequestLens = Body.auto<UsbWeighingData>().toLens()
    private val weighingResultLens = Body.auto<UsbWeighingResult>().toLens()
    override fun invoke(request: Request): Response {
        val weighingRequest = weighingRequestLens.extract(request)
        val vidPid = deviceHolder.useDevice(DeviceType.LIBRAUSB) { it.vidPidKey }
                ?: throw ApiException.deviceNotFound("Весы не подключены")

        val currentValue = weighingBus.take(1).blockingFirst()
        val finalValue = if (weighingRequest.weight != null) {
            val oldWeight = weighingRequest.weight.toBigDecimal().divide(BigDecimal(1000))
            if (currentValue.compareTo(oldWeight) == 0)
                try {
                    weighingBus
                            .skip(1)
                            .timeout(weighingRequest.timeout.toLong(), TimeUnit.SECONDS)
                            .blockingFirst()
                } catch (e: Throwable) {
                    when (e) {
                        is TimeoutException, is NoSuchElementException ->
                            currentValue
                        else -> throw ApiException.fromUnexpectedException(e, vidPid)
                    }
                }
            else {
                currentValue
            }
        } else {
            currentValue
        }
        val result = UsbWeighingResult((finalValue * BigDecimal(1000)).intValueExact(), vidPid)
        return weighingResultLens.inject(result, Response(Status.OK))
    }
}