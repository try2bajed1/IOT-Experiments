package iot.example.devices.v2

import com.google.gson.GsonBuilder
import org.http4k.format.ConfigurableGson
import org.joda.time.DateTime

object ServerGson : ConfigurableGson(GsonBuilder()
        .registerTypeAdapter(DateTime::class.java, GsonDateTimeConverter())
        .registerSerializers()
        .serializeNulls()
        .excludeFieldsWithoutExposeAnnotation())