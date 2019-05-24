package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Observable
import io.reactivex.Scheduler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import iot.example.devices.DISPATCH_SCHEDULER
import iot.example.devices.WEIGHING_BUS_TAG
import iot.example.devices.v2.ServerGson.auto
import java.math.BigDecimal

class WeighingWSBus(injector: Kodein) : WsConsumer {

    private val weighingBus = injector.instance<Observable<BigDecimal>>(WEIGHING_BUS_TAG)
    private val weighingResultLens = WsMessage.auto<WeighingResult>().toLens()
    private val dispatchScheduler: Scheduler = injector.instance(DISPATCH_SCHEDULER)

    override fun invoke(socket: Websocket) {
        val subscription = weighingBus
                .distinctUntilChanged { f, s -> f.compareTo(s) == 0 }
                .observeOn(dispatchScheduler)
                .subscribe { res ->
                    val result = WeighingResult(res)
                    val message = weighingResultLens.create(result)
                    socket.send(message)
                }

        socket.onClose {
            subscription.dispose()
        }
        socket.onError {
            socket.close()
        }
    }
}