package iot.example.devices

import android.system.Os.bind
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.bindings.NoArgBindingKodein
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import iot.example.devices.hardware.DeviceHolder
import iot.example.devices.resolvers.*
import iot.example.devices.server.*
import iot.example.devices.v2.*
import java.math.BigDecimal
import java.util.Collections.singleton
import java.util.logging.Logger
import javax.security.auth.Subject

val httpModule = Kodein.Module {
    simpleSingleton { ScannerHandler(kodein) }
    simpleSingleton { BeaconHandler(kodein) }
    simpleSingleton { LibraHandler(kodein) }
    simpleSingleton { LibraUsbHandler(kodein) }
}

val v2Module = Kodein.Module {
    simpleSingleton { V2Router(kodein) }
    simpleSingleton { WSRouter(kodein) }
    simpleSingleton { DeviceWSBus(kodein) }
    simpleSingleton { ScanningWSBus(kodein) }
    simpleSingleton { WeighingWSBus(kodein) }
    simpleSingleton { DeviceInfoHandler(kodein) }
}
val hardwareModule = Kodein.Module {

    bind<Logger>() with multiton { cls: Class<*> -> LoggerFactory.getLogger(cls) }
    simpleSingleton { UsbDevicesScanner(kodein) }
    simpleSingleton { DeviceManager(kodein) }
    simpleSingleton { DeviceHolder() }

    bind() from setBinding<Resolver<*>>()
    bind<Resolver<*>>().inSet() with singleton { ScannerResolver(kodein) }
    bind<Resolver<*>>().inSet() with singleton { LibraResolver(kodein) }
    bind<Resolver<*>>().inSet() with singleton { LibraUsbResolver(kodein) }
}

val configModule = Kodein.Module {
    bind<Config>() with singleton { ConfigFactory.load() }
}

private inline fun <reified T : Any> Kodein.Builder.simpleSingleton(noinline creator: NoArgBindingKodein.() -> T) =
        bind<T>() with singleton(creator)

const val BARCODE_BUS_TAG = "barcodeBusTag"
const val WEIGHING_BUS_TAG = "weighingBusTag"
const val DISPATCH_SCHEDULER = "DISPATCH_SCHEDULER"

val busModule = Kodein.Module {
    val barcodeBus = PublishSubject.create<String>()
    bind<Subject<String>>(BARCODE_BUS_TAG) with instance(barcodeBus)
    bind<Observable<String>>(BARCODE_BUS_TAG) with instance(barcodeBus)

    val weighingBus = BehaviorSubject.createDefault<BigDecimal>(BigDecimal.ZERO)
    bind<Subject<BigDecimal>>(WEIGHING_BUS_TAG) with instance(weighingBus)
    bind<Observable<BigDecimal>>(WEIGHING_BUS_TAG) with instance(weighingBus)

    //Используется для синхронной отправки сообщений в сторогом порядке
    bind<Scheduler>(DISPATCH_SCHEDULER) with singleton { Schedulers.single() }
}