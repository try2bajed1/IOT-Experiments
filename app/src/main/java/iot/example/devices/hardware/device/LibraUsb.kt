package iot.example.devices.device

import android.hardware.usb.UsbDevice
import com.google.android.things.pio.UartDevice.PARITY_EVEN
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject
import jssc.SerialPort.*
import iot.example.devices.DeviceType
import iot.example.devices.SerialException
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.logging.Logger.getLogger
import javax.security.auth.Subject
import javax.usb.UsbDevice

class LibraUsb(vid: Int, pid: Int,
               usbDevice: UsbDevice,
               private val subject: Subject<BigDecimal>,
               refreshInterval: Duration,
               serial: Serial = DefaultSerial(TIMEOUT, "/dev/ttyACM0", BAUDRATE_4800,
                       DATABITS_8, STOPBITS_1, PARITY_EVEN))
    : Device<LibraUsb>(vid, pid, usbDevice, DeviceType.LIBRAUSB, "Весы"), Serial by serial {

    private val subscription: Disposable

    init {
        val fast = ByteArray(10)    // как правило у нас 10 байт ответа
        val buffer = ByteArray(100)
        subscription = Observable.interval(refreshInterval.toMillis(), TimeUnit.MILLISECONDS)
                .subscribe {
                    try {
                        val data = readData(buffer, fast)
                        if (data != null) {
                            val result = parseWeightingResult(data)
                            if (result != null)
                                subject.onNext(result)
                        }
                    } catch (e: Throwable) {
                        logger.error("Error, while reading data.", e)
                        stopScheduling()
                    }
                }
    }

    private fun stopScheduling() {
        subscription.dispose()
    }

    override fun detached() {
        try {
            stop()
        } catch (e: SerialException) {
            logger.error("Error, while closing port.", e)
        }
    }

    private fun parseWeightingResult(data: ByteArray): BigDecimal? {
        if (data.size != 10)
            return null
        if (data[0].unsignedByte().toInt() != 0x55 || data[1].unsignedByte().toInt() != 0xAA)
        // префикс
            return null
        if (data[4].unsignedByte().toInt() != 0x00 && data[4].unsignedByte().toInt() != 0x80)
        // знак
            return null
        if (data[0] != data[5] || data[1] != data[6] || data[2] != data[7] || data[3] != data[8] || data[4] != data[9])
            return null

        val value = data[2].unsignedByte().toInt() + (data[3].unsignedByte().toInt() shl 8)
        val result = if (data[4].unsignedByte().toInt() == 0x80) -value else value
        return BigDecimal(result).divide(BigDecimal(1000))
    }

    //TODO переделать нормально, легаси логика
    private fun readData(buffer: ByteArray, fast: ByteArray): ByteArray? {
        var startTime = System.currentTimeMillis()
        var len = 0
        do {
            val data = serialPort.readBytes()
            if (data != null && len + data.size < buffer.size) {
                System.arraycopy(data, 0, buffer, len, data.size)
                len += data.size
                if (len == 10)
                // как правило у нас 10 байт ответа
                    break
                startTime = System.currentTimeMillis() // ждем еще немного
            }
        } while (System.currentTimeMillis() - startTime < TIMEOUT)
        if (len == 0)
            return null
        val ret = if (len == 10) fast else ByteArray(len)
        System.arraycopy(buffer, 0, ret, 0, len)
        return ret
    }

    companion object {
        private const val TIMEOUT = 100L
        private val logger = getLogger()
    }
}