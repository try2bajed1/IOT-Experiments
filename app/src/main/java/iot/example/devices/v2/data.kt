package iot.example.devices.v2

import iot.example.devices.DeviceType


fun Device<*>.getDeviceInfo() =
        DeviceInfo(devicePid = pid.toHexShortFormat(),
                deviceVid = vid.toHexShortFormat(),
                deviceTypeId = deviceType.toApiDeviceType()!!,
                deviceName = deviceName)

fun DeviceType<*>.toApiDeviceType() = ApiDeviceType.getById(this.typeCode)