package iot.example.devices

import iot.example.devices.device.*

class DeviceType<T : Device<T>> private constructor(val clazz: Class<T>,
                                                    val typeName: String,
                                                    val typeCode: Int) {
    @Suppress("UNCHECKED_CAST")
    companion object {
        val SCANNER = DeviceType(Scanner::class.java, "Scanner", ApiDeviceType.SCANNER.id)
        val LIBRA = DeviceType(Libra::class.java, "Libra", ApiDeviceType.LIBRA.id)
        val LIBRAUSB = DeviceType(LibraUsb::class.java, "LibraUsb", ApiDeviceType.LIBRAUSB.id)

        fun <T : Device<T>> getTypeByClass(clazz: Class<T>): DeviceType<T>? =
                clazzToType[clazz] as DeviceType<T>

        val allTypes: List<DeviceType<*>> = listOf(SCANNER,  LIBRA,  LIBRAUSB)

        private val clazzToType: Map<Class<out Device<*>>, DeviceType<*>> = allTypes.associateBy { it.clazz }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceType<*>

        if (clazz != other.clazz) return false
        if (typeName != other.typeName) return false
        if (typeCode != other.typeCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + typeName.hashCode()
        result = 31 * result + typeCode
        return result
    }

    override fun toString(): String {
        return "DeviceType(clazz=$clazz, typeName='$typeName', typeCode=$typeCode)"
    }


}