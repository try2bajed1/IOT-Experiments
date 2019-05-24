package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.reactivex.Scheduler
import iot.example.devices.Revision
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import iot.example.devices.DISPATCH_SCHEDULER
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.v2.ServerGson.auto

class DeviceWSBus(injector: Kodein) : WsConsumer {
    private val holder: DeviceHolder = injector.instance()
    private val deviceInfoLens = WsMessage.auto<BoxInfo>().toLens()
    private val dispatchScheduler: Scheduler = injector.instance(DISPATCH_SCHEDULER)

    override fun invoke(socket: Websocket) {
        logger.info("New income WS connection: ${socket.upgradeRequest.toMessage()}")
        val subscription = holder.deviceInfoBus
                .observeOn(dispatchScheduler)
                .subscribe { stateInfo ->
                    logger.debug("Sending state $stateInfo")
                    val info = BoxInfo(
                        Revision.VERSION,
                            stateInfo)
                    val message: WsMessage = deviceInfoLens(info)
                    socket.send(message)
                }
        socket.onClose {
            subscription.dispose()
            logger.info("Closing WS connection")
        }
        socket.onError {
            socket.close()
            logger.error("Error occurred while doing WS work.", it)
        }
    }

    private companion object {
        val logger = DeviceWSBus.getLogger()
    }
}