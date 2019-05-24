package iot.example.devices.device

import android.provider.SyncStateContract
import jssc.SerialPort
import jssc.SerialPortList
import org.funktionale.either.eitherTry
import iot.example.devices.SerialException
import java.util.logging.Logger.getLogger

class DefaultSerial(private val readTimeout: Long,
                    portPrefix: String,
                    private val baudrate: Int,
                    private val databits: Int,
                    private val stopbits: Int,
                    private val parity: Int) : Serial {

    override val serialPort: SerialPort

    init {                         //     /dev/ttyACM0          /dev/ttyAMA0
        var port: String? = null
        // tty port в системе создается не сразу. попробуем потыркаться 10 секунд
        for (j in 0..19) {
            val portNames = SerialPortList.getPortNames()
            var i = 0
            while (i < portNames.size && port == null) {
                if (portNames[i].startsWith(portPrefix))
                    port = portNames[i]
                i++
            }
            if (port != null)
                break
            Thread.sleep(500)
        }
        Thread.sleep(1000) // еще поспим. дадим порту созреть
        if (port == null)
            throw SerialException("Can't init any tty port", SyncStateContract.Constants.ERROR_NO_DEVICE)
        serialPort = SerialPort(port)
        eitherTry {
            serialPort.openPort()
            serialPort.setParams(baudrate, databits, stopbits, parity)
        }.foldLeft {
            logger.error("Can't perform port init.")
            throw SerialException("Can't perform port init.", SyncStateContract.Constants.ERROR_UNKNOWN, cause = it)
        }
        purge()
    }

    override fun purge() {
        eitherTry {
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_TXCLEAR)
        }
                .foldLeft {
                    throw SerialException("Can't purge port.", SyncStateContract.Constants.ERROR_UNKNOWN, cause = it)
                }
    }

    override fun write(data: ByteArray) {
        eitherTry { serialPort.writeBytes(data) }
                .mapLeft {
                    throw SerialException("Can't write to port.", SyncStateContract.Constants.ERROR_UNKNOWN, cause = it)
                }
                .foldRight { success ->
                    if (!success)
                        throw SerialException("Can't write to port.", SyncStateContract.Constants.ERROR_UNKNOWN)
                }
    }

    override fun read(): ByteArray? {
        try {
            val startTime = System.currentTimeMillis()
            do {
                Thread.yield()
                val data = serialPort.readBytes()

                if (data != null) {
                    return data
                }
            } while (System.currentTimeMillis() - startTime < readTimeout)
            return null
        } catch (e: Exception) {
            throw SerialException("Can't read from port.", SyncStateContract.Constants.ERROR_UNKNOWN)
        }
    }

    override fun stop() {
        try {
            serialPort.closePort()
        } catch (e: Exception) {
            SerialException("Error, while closing port.", cause = e)
        }
    }

    companion object {
        private val logger = getLogger()
    }

}