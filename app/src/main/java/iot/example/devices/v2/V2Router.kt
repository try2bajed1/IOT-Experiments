package iot.example.devices.v2

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.routing.PathMethod
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import iot.example.devices.Router
import iot.example.devices.server.ContentTypeFilter
import java.lang.reflect.Method
import kotlin.reflect.KClass

class V2Router(private val injector: Kodein) : Router {
    override fun invoke(): RoutingHttpHandler = routes(
            //todo:add new routes

    ).let { routes ->
        ContentTypeFilter
                .then(V2ErrorFilter)
                .then(routes)
    }


    private inline infix fun <reified T : Router> String.bind(clazz: KClass<T>) =
            bind(injector.instance<T>().invoke())

    private inline infix fun <reified T : HttpHandler> PathMethod.with(clazz: KClass<T>) =
            to(injector.instance<T>())
}