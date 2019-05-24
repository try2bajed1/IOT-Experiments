package iot.example.devices.resolvers

import iot.example.devices.device.Device
import javax.usb.UsbDevice

interface Resolver<T : Device<T>> {
    fun isAcceptable(vid: Int, pid: Int): Boolean
    fun newDevice(vid: Int, pid: Int, device: UsbDevice): T
}