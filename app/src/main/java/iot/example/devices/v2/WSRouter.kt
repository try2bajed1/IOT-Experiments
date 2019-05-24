package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.websocket.WsConsumer
import kotlin.reflect.KClass

class WSRouter(private val injector: Kodein) : () -> RoutingWsHandler {
    override fun invoke() = websockets(
            "/info" bind DeviceWSBus::class,
            "/scan" bind ScanningWSBus::class,
            "/weight" bind WeighingWSBus::class
    )

    private inline infix fun <reified T : WsConsumer> String.bind(clazz: KClass<T>): RoutingWsHandler =
            this bind injector.instance<T>()
}