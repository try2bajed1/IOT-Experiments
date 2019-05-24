package iot.example.devices

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.typesafe.config.Config
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.routing.PathMethod
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.PolyHandler
import iot.example.devices.UsbDevicesScanner
import iot.example.devices.server.*
import iot.example.devices.v2.V2Router
import iot.example.devices.v2.WSRouter
import java.lang.reflect.Method
import java.util.logging.Logger.getLogger
import kotlin.reflect.KClass





object App {
    private val logger = getLogger()
    @JvmStatic
    fun main(args: Array<String>) {

        logger.info("\n\n\n===============\n\n\n")
        logger.info("Starting server")
        val injector = kodein
        logger.info("Starting usb scan.")
        injector.instance<UsbDevicesScanner>().startScanUsbDevices()

        logger.info("Preparing HTTP server.")

        val wsRouter = injector.instance<WSRouter>()
        val wsApp = websockets(
                "/ws/v1" bind wsRouter()
        )

        val httpApp = routes(
                "/scan" bind Method.POST with ScannerHandler::class,
                "/ping" bind Method.GET with BeaconHandler::class,
                "/weight" bind Method.GET with LibraHandler::class,
                "/weightusb" bind Method.POST with LibraUsbHandler::class,
                "/v2" bind injector.instance<V2Router>().invoke()
        )
                .let { routes ->
                    ContentTypeFilter
                            .then(ExceptionFilter)
                            .then(routes)
                }
        val port = injector.instance<Config>().getInt("server.port")
        val jettyServer = PolyHandler(httpApp, wsApp).asServer(Jetty(port)).start()

        logger.info("Starting HTTP server.")
        jettyServer.start()

        logger.info("Started")
        jettyServer.block()
    }



    private inline infix fun <reified T : HttpHandler> PathMethod.with(clazz: KClass<T>) =
            to(kodein.instance<T>())

    private val kodein: Kodein by lazy {
        Kodein {
            import(httpModule)
            import(v2Module)
            import(hardwareModule)
            import(configModule)
            import(busModule)
        }
    }

}