package iot.example.devices.server

import com.google.gson.GsonBuilder
import org.http4k.format.ConfigurableGson
import org.joda.time.DateTime

object ServerGson : ConfigurableGson(GsonBuilder()
        .registerTypeAdapter(DateTime::class.java, GsonDateTimeConverter())
        .serializeNulls()
        .excludeFieldsWithoutExposeAnnotation())