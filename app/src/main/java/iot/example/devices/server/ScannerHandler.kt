package iot.example.devices.server

import android.icu.text.DateTimePatternGenerator.PatternInfo.OK
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Observable
import org.funktionale.either.eitherTry
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import iot.example.devices.BARCODE_BUS_TAG
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.DeviceType
import iot.example.devices.server.ServerGson.auto
import iot.example.devices.server.pojo.ScanningData
import iot.example.devices.server.pojo.ScanningRequest
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ScannerHandler(injector: Kodein) : HttpHandler {
    private val barcodeObservable: Observable<String> = injector.instance(BARCODE_BUS_TAG)
    private val deviceHolder: DeviceHolder = injector.instance()
    private val requestLens = Body.auto<ScanningRequest>().toLens()
    private val responseLens = Body.auto<ScanningData>().toLens()

    override fun invoke(req: Request): Response {
        val request = requestLens.extract(req)
        val vidPidKey = deviceHolder.useDevice(DeviceType.SCANNER) { it.vidPidKey }
                ?: throw ApiException.deviceNotFound("Сканнер не подключен")
        val barcode = eitherTry {
            barcodeObservable
                    .firstOrError()
                    .timeout(request.timeout.toLong(), TimeUnit.SECONDS)
                    .blockingGet()
        }.foldLeft { ex ->
            if (ex is TimeoutException)
            //Возращаем пустую строку
                ""
            else
                throw ApiException.fromUnexpectedException(ex, vidPidKey)
        }

        val data = ScanningData(barcode = barcode,
                deviceId = vidPidKey)
        return responseLens.inject(data, Response(OK))
    }
}