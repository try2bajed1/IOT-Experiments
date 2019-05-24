package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Observable
import io.reactivex.Scheduler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import iot.example.devices.BARCODE_BUS_TAG
import iot.example.devices.DISPATCH_SCHEDULER
import iot.example.devices.v2.ServerGson.auto

class ScanningWSBus(injector: Kodein) : WsConsumer {
    private val bus: Observable<String> = injector.instance(BARCODE_BUS_TAG)
    private val deviceInfoLens = WsMessage.auto<ScanningResult>().toLens()
    private val dispatchScheduler: Scheduler = injector.instance(DISPATCH_SCHEDULER)

    override fun invoke(socket: Websocket) {
        logger.info("New income WS connection: ${socket.upgradeRequest.toMessage()}")
        val subscription = bus
                .observeOn(dispatchScheduler)
                .subscribe {
                    val result = ScanningResult(barcode = it)
                    val message = deviceInfoLens(result)
                    socket.send(message)
                }
        socket.onClose {
            subscription.dispose()
            logger.info("Closing WS connection")
        }
        socket.onError {
            socket.close()
            logger.warn("Error occurred while doing WS work.", it)
        }
    }

    private companion object {
        val logger = ScanningWSBus.getLogger()
    }
}