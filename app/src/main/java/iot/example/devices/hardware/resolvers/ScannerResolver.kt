package iot.example.devices.resolvers

import android.hardware.usb.UsbDevice
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.typesafe.config.Config
import io.reactivex.subjects.Subject
import iot.example.devices.BARCODE_BUS_TAG
import iot.example.devices.device.Scanner
import javax.security.auth.Subject
import javax.usb.UsbDevice

class ScannerResolver(kodein: Kodein) : Resolver<Scanner> {

    private val barcodeSubject: Subject<String> = kodein.instance(BARCODE_BUS_TAG)
    private val debounceTime = kodein.instance<Config>().getDuration("scanner.debounceTime")

    override fun isAcceptable(vid: Int, pid: Int): Boolean =
            supportedIds.contains(createVidPidKey(vid, pid))

    override fun newDevice(vid: Int, pid: Int, device: UsbDevice): Scanner =
            Scanner(vid, pid, device, barcodeSubject, debounceTime).also { it.startReading() }


    private companion object {
        val supportedIds = listOf(
                VID_STMJOY_5800 to PID_STMJOY_5800,
                VID_STMJOY to PID_STMJOY,
                VID_AUTHENTEC to PID_AUTHENTEC,
                VID_SYMBOL to PID_SYMBOL,
                VID_SHANGCHEN to PID_SHANGCHEN,
                VID_FUZZYSCAN to PID_FUZZYSCAN,
                VID_24G_RF_KEYBOARD to PID_24G_RF_KEYBOARD,
                VID_ARGOX to PID_ARGOX,
                VID_PCPLAY to PID_PCPLAY,
                VID_SCANNER_UNKNOWN to PID_SCANNER_UNKNOWN,
                VID_METROLOGIC to PID_METROLOGIC,
                VID_METROLOGIC_MERCURY to PID_METROLOGIC_MERCURY,
                VID_METROLOGIC_MS7120 to PID_METROLOGIC_MS7120,
                VID_HONEYWELL to PID_HONEYWELL,
                0x23d0 to 0x0b8f,
                0x0745 to 0x0000,
                0x23d0 to 0x0c21,
                0x1388 to 0x1388,
                0x046d to 0xc534,
                0x046d to 0xc534,
                0x10c4 to 0xff11, //viatech scanner
                0x0c2e to 0x0b41,
                0xffff to 0x0035, //atol sb1 103 by usb
                0x08ff to 0x0009 //atol sb1 103 by bt
        )
                .map { createVidPidKey(it.first, it.second) }
                .toSet()
    }
}