package iot.example.devices

import android.bluetooth.BluetoothClass
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import iot.example.devices.DeviceType.Companion.LIBRA
import iot.example.devices.DeviceType.Companion.LIBRAUSB
import iot.example.devices.DeviceType.Companion.SCANNER
import iot.example.devices.hardware.DeviceHolder
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

object DeviceHolderTest : Spek({

    describe("A holder") {
        lateinit var holder: DeviceHolder
        lateinit var eventHistory: Observable<List<DeviceInfo>>
        lateinit var eventSubject: PublishSubject<List<DeviceInfo>>

        beforeEachTest {
            holder = DeviceHolder()
            eventSubject = PublishSubject.create()
            eventHistory = eventSubject.replay().also { it.connect() }
            holder.deviceInfoBus.subscribe {
                eventSubject.onNext(it)
            }
        }

        on("initial state") {

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("must be empty") {
                holder.getDevicesState().shouldBeEmpty()
            }

            it("must return null on device usage") {
                holder.useDevice(SCANNER) { it }.shouldBeNull()
                holder.useDevice(LIBRA) { it }.shouldBeNull()
                holder.useDevice(LIBRAUSB) { it }.shouldBeNull()
            }

            it("init event bus must has empty value") {
                eventResult.size shouldEqual 1
                eventResult[0].shouldBeEmpty()
            }
        }

        on("inserting and using new device") {
            val deviceMockType = DeviceType.SCANNER

            holder.addDevice(deviceMock)

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("should be suitable for using that device type") {
                val result = holder.useDevice(DeviceType.SCANNER) { it }
                result.shouldNotBeNull()
                result shouldBe deviceMock
            }

            it("should not be suitable for using another device types") {
                holder.useDevice(SCANNER) { it }.shouldBeNull()
                holder.useDevice(LIBRA) { it }.shouldBeNull()
                holder.useDevice(LIBRAUSB) { it }.shouldBeNull()
            }

            it("should send valid event to event bus") {
                eventResult.size shouldEqual 2
                val resultEvent = eventResult[1]
                resultEvent shouldEqual listOf(deviceMock.getDevice())
            }
        }

        on("trying to remove not present device by vid/pid") {

            val removingResult = holder.removeDevice(0, 0)

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("should return null") {
                removingResult.shouldBeNull()
            }

            it("should send only init event") {
                eventResult.size shouldBe 1
                eventResult[0].shouldBeEmpty()
            }
        }

        on("trying to remove not present device by type") {

            val removingResult = holder.removeDevice(LIBRA)

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("should return null") {
                removingResult.shouldBeNull()
            }

            it("should send only init event") {
                eventResult.size shouldBe 1
                eventResult[0].shouldBeEmpty()
            }
        }

        on("removing present device by vid/pid") {
            val deviceMockType = DeviceType.LIBRA
            val deviceMock: BluetoothClass.Device<*> = createDeviceMock(deviceMockType)

            holder.addDevice(deviceMock)
            val removingResult = holder.removeDevice(deviceMock.vid, deviceMock.pid)

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("should return added device on returning") {
                deviceMock shouldBe removingResult
            }

            it("should return null on any usage") {
                holder.useDevice(SCANNER) { it }.shouldBeNull()
                holder.useDevice(LIBRA) { it }.shouldBeNull()
            }

            it("should sent valid event to event bus") {
                eventResult.size shouldEqual 3
                val resultEvent = eventResult[2]
                resultEvent.shouldBeEmpty()
            }
        }

        on("removing present device by type") {
            val deviceMockType = DeviceType.LIBRA
            val deviceMock: BluetoothClass.Device<*> = createDeviceMock(deviceMockType)

            holder.addDevice(deviceMock)
            val removingResult = holder.removeDevice(deviceMockType)

            eventSubject.onComplete()
            val eventResult = eventHistory.toList().blockingGet()

            it("should return added device on returning") {
                deviceMock shouldBe removingResult
            }

            it("should return null on any usage") {
                holder.useDevice(SCANNER) { it }.shouldBeNull()
                holder.useDevice(LIBRA) { it }.shouldBeNull()
                holder.useDevice(LIBRAUSB) { it }.shouldBeNull()
            }

            it("should sent valid event to event bus") {
                eventResult.size shouldEqual 3
                val resultEvent = eventResult[2]
                resultEvent.shouldBeEmpty()
            }
        }

        context("multi-threading tests") {
            lateinit var executor: ExecutorService
            beforeEachTest {
                executor = Executors.newFixedThreadPool(4)
            }

            afterEachTest {
                executor.shutdown()
            }


            it("on removing by vid/pid must await until work will be completed") {
                val deviceMockType = DeviceType.SCANNER
                val deviceMock: BluetoothClass.Device<*> = createDeviceMock(deviceMockType)
                val lock: Lock = ReentrantLock()

                holder.addDevice(deviceMock)

                val removingEventListener = executor.submit(Callable { eventHistory.take(3).toList().blockingGet() })

                lock.lock()
                executor.submit {
                    holder.useDevice(deviceMockType) {
                        lock.tryLock(1000, TimeUnit.MILLISECONDS)
                    }
                }
                val removingResult = executor.submit(Callable { holder.removeDevice(deviceMock.vid, deviceMock.pid) })

                try {
                    removingResult.isDone shouldEqualTo false
                    //Не сможет удалить устройство, пока идёт использование              
                    removingResult.get(50, TimeUnit.MILLISECONDS)
                    Assert.fail("Removing was success, but expected, that not")
                } catch (t: TimeoutException) {
                    //Значит, всё правильно
                }

                removingEventListener.isDone shouldEqualTo false

                lock.unlock()

                val result = removingResult.get(50, TimeUnit.MILLISECONDS)
                result.shouldNotBeNull()
                result!! shouldBe deviceMock

                eventSubject.onComplete()

                val event = removingEventListener.get(50, TimeUnit.MILLISECONDS)
                event[0].shouldBeEmpty()
                event[1] shouldEqual listOf(deviceMock.getDeviceInfo())
                event[2].shouldBeEmpty()

                event shouldEqual eventHistory.toList().blockingGet()
            }

            it("must await until another work will be completed") {
                val deviceMockType = DeviceType.SCANER
                val deviceMock: BluetoothClass.Device<*> = createDeviceMock(deviceMockType)
                val lock: Lock = ReentrantLock()

                holder.addDevice(deviceMock)

                lock.lock()
                executor.submit {
                    holder.useDevice(deviceMockType) {
                        lock.tryLock(1000, TimeUnit.MILLISECONDS)
                    }
                }
                val usageResult = "Result"
                val removingResult = executor.submit(Callable { holder.useDevice(s) { usageResult } })

                try {
                    removingResult.isDone shouldEqualTo false
                    //Не сможет удалить устройство, пока идёт использование
                    removingResult.get(50, TimeUnit.MILLISECONDS)
                    Assert.fail("Removing was success, but expected, that not")
                } catch (t: TimeoutException) {
                    //Значит, всё правильно
                }

                lock.unlock()

                val result = removingResult.get(50, TimeUnit.MILLISECONDS)
                result.shouldNotBeNull()
                result!! shouldBe usageResult
            }
        }
    }
})

inline fun <reified T : BluetoothClass.Device<T>> createDeviceMock(deviceMockType: DeviceType<T>) =
        if (deviceMockType == LIBRA) {
            mock<FiscalPrinter> {
                on { deviceType }.thenReturn(LIBRA)
                on { deviceName }.thenReturn("libra")
                on { vid }.thenReturn(0)
                on { pid }.thenReturn(0)
                on { receiptWidth }.thenReturn(32)
                on { printerIndex }.thenReturn(32)
                on { fnNumber }.thenReturn("ddef")
                on { factoryKktNumber }.thenReturn("fefe")
                on { registrationNumber }.thenReturn("fedge")
            }
        } else
            mock<T> {
                on { deviceType }.thenReturn(deviceMockType)
                on { deviceName }.thenReturn(deviceMockType.typeName)
                on { vid }.thenReturn(0)
                on { pid }.thenReturn(0)
            }