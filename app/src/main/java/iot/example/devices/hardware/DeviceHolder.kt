package iot.example.devices.hardware

import iot.example.devices.DeviceType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Потокобезопасное хранилище активных девайсов.
 * Два потока не могут одновременно получить доступ к девайсу.
 */
class DeviceHolder {

    private val devices: ConcurrentMap<DeviceType<*>, Pair<Device<*>, Lock>> = ConcurrentHashMap()
    private val writeLock: Lock = ReentrantLock()
    private val eventSubject: BehaviorSubject<List<DeviceInfo>> = BehaviorSubject.createDefault(emptyList())
    val deviceInfoBus: Observable<List<DeviceInfo>> = eventSubject

    //Должен быть использован ТОЛЬКО под write локом
    private fun sendStateUpdate() {
        eventSubject.onNext(devices.values.map { it.first.getDeviceInfo() })
    }

    /**
     * Добавляет девайс в хранилище.
     */
    fun addDevice(device: Device<*>): Unit =
            writeLock.withLock {
                devices.put(device.deviceType, device to ReentrantLock())
                sendStateUpdate()
            }

    /**
     * Открывает девайс для использования.
     * Пока девайс используется, он не будет выдан другим потокам.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Device<T>, R> useDevice(deviceType: DeviceType<T>, usage: (T) -> R): R? =
            devices[deviceType]?.let {
                it.second.withLock {
                    //Проверяется, что девайс не был удалён
                    if (devices.contains(deviceType))
                        usage(it.first as T)
                    else
                        null
                }
            }

    /**
     * Удаляет девайс из хранилища.
     * Пока девайс будет использоваться, он не может быть удалён.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Device<T>> removeDevice(type: DeviceType<T>): T? =
            devices[type]?.let {
                it.second.withLock {
                    //Если уже был удалён, то и не важно
                    writeLock.withLock {
                        (devices.remove(type)?.first as T?)
                                ?.also { sendStateUpdate() }
                    }
                }
            }

    /**
     * Удаляет девайс из хранилища.
     * Пока девайс будет использоваться, он не может быть удалён.
     */
    fun removeDevice(vid: Int, pid: Int): Device<*>? =
            devices.values
                    .firstOrNull { it.first.vidPidKey == createVidPidKey(vid, pid) }
                    ?.let { pair ->
                        pair.second.withLock {
                            //Если уже был удалён, то и не важно
                            writeLock.withLock {
                                devices.remove(pair.first.deviceType)?.first
                                        ?.also { sendStateUpdate() }
                            }
                        }
                    }


    /**
     * Выдаёт текущий стейт
     */
    fun getDevicesState(): List<DeviceInfo> = eventSubject.value



}