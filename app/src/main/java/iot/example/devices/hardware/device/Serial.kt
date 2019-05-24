package iot.example.devices.device

import jssc.SerialPort
import iot.example.devices.SerialException

interface Serial {

    val serialPort: SerialPort
    @Throws(SerialException::class)
    fun purge()

    @Throws(SerialException::class)
    fun write(data: ByteArray)

    @Throws(SerialException::class)
    fun read(): ByteArray?

    @Throws(SerialException::class)
    fun stop()

}