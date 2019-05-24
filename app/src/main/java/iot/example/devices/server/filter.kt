package iot.example.devices.server

import android.provider.SyncStateContract
import org.http4k.core.*
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import iot.example.devices.server.ServerGson.auto
import iot.example.devices.server.pojo.Error
import java.util.logging.Logger.getLogger

object ExceptionFilter : Filter {
    private val errorLens = Body.auto<Error>().toLens()
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        try {
            next(request)
        } catch (t: ApiException) {
            logger.info("Expected error.", t)
            Error(code = t.code,
                    deviceId = t.deviceId,
                    recoverable = t.recoverable,
                    message = t.message,
                    result = t.data,
                    log = t.stackTrace.map { it.toString() })
                    .let { errorLens.inject(it, Response(Status.OK)) }
        } catch (e: LensFailure) {
            logger.warn("Lens error occurred.", e)
            Error(code = SyncStateContract.Constants.ERROR_UNKNOWN,
                    deviceId = "????:????",
                    recoverable = true,
                    message = e.failures.joinToString(";"),
                    result = null,
                    log = e.cause?.stackTrace?.map { it.toString() } ?: emptyList())
                    .let {
                        if (e.overall() == Failure.Type.Unsupported)
                            errorLens.inject(it, Response(Status.UNSUPPORTED_MEDIA_TYPE))
                        else
                            errorLens.inject(it, Response(Status.OK))
                    }
        } catch (t: Throwable) {
            logger.errToSenrty("Unexpected error.", t)
            Error(code = SyncStateContract.Constants.ERROR_UNKNOWN,
                    deviceId = "????:????",
                    recoverable = true,
                    message = t.message ?: "Неизвестная ошибка",
                    log = t.stackTrace.map { it.toString() })
                    .let { errorLens.inject(it, Response(Status.INTERNAL_SERVER_ERROR)) }
        }
    }

    private val logger = getLogger()
}

object ContentTypeFilter : Filter {
    private val applicationTypeHeaderLens = Header.required("Content-Type")
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        applicationTypeHeaderLens.inject(ContentType.APPLICATION_JSON.toHeaderValue(), next(request))
    }

}