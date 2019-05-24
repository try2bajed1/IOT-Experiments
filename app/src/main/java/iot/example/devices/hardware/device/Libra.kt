package iot.example.devices.device

import android.hardware.usb.UsbDevice
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject
import jssc.SerialPort
import iot.example.devices.DeviceType
import iot.example.devices.LibraException
import iot.example.devices.SerialException
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.logging.Logger.getLogger
import javax.security.auth.Subject
import javax.usb.UsbDevice

class Libra(vid: Int, pid: Int,
            usbDevice: UsbDevice,
            private val subject: Subject<BigDecimal>,
            refreshInterval: Duration,
            serial: Serial = DefaultSerial(TIMEOUT, portPrefix, baudrate, databits, stopbits, parity)) :
        Device<Libra>(vid, pid, usbDevice, DeviceType.LIBRA, "Весы"), Serial by serial {

    private val subscription: Disposable = Observable.interval(refreshInterval.toMillis(), TimeUnit.MILLISECONDS)
            .subscribe {
                try {
                    subject.onNext(getWeight())
                } catch (e: Throwable) {
                    logger.error("Error, while doing weighing.", e)
                    stopScheduling()
                }
            }

    private fun stopScheduling() {
        subscription.dispose()
    }

    private fun getWeight(): BigDecimal {
        try {
            write(cmd)
            for (i in 0..9) {
                val reply: ByteArray = read() ?: continue
                if (reply.size != 2)
                    throw LibraException("Error, can't read bytes. Expected 2, got ${reply.size}", deviceId = vidPidKey)
                val result = Util.unsignedByte(reply[0]).toInt() + Util.shiftLeft(reply[1], 8)
                return BigDecimal(result).divide(BigDecimal(1000))
            }
        } catch (ex: SerialException) {
            logger.error("Error, while working with serial device.", ex)
            throw LibraException("Error, while working with serial device.",
                    code = ex.code,
                    recoverable = ex.recoverable,
                    deviceId = vidPidKey,
                    cause = ex)
        }
        throw LibraException("Error, can't read bytes. Tried 10 times.", deviceId = vidPidKey)
    }

    override fun detached() {
        try {
            subscription.dispose()
            stop()
        } catch (e: SerialException) {
            logger.error("Error, while closing port.", e)
        }
    }

    companion object {
        private val logger = getLogger()
        private val TIMEOUT = 1000L
        private val baudrate = SerialPort.BAUDRATE_4800
        private val databits = SerialPort.DATABITS_8
        private val stopbits = SerialPort.STOPBITS_1
        private val parity = SerialPort.PARITY_EVEN
        private val cmd = byteArrayOf(0x45)
        private val portPrefix = "/dev/ttyUSB"
    }
}
