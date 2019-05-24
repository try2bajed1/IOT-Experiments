package iot.example.devices.device

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import iot.example.devices.DeviceType
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger.getLogger
import javax.security.auth.Subject
import javax.usb.*
import kotlin.concurrent.thread

open class Scanner(vid: Int, pid: Int,
                   usbDevice: UsbDevice,
                   barcodeSubject: Subject<String>,
                   debounceTime: Duration) :
        Device<Scanner>(vid, pid, usbDevice, DeviceType.SCANNER, "Сканнер") {
    
    private val debounceSubject: Subject<String> = PublishSubject.create()
    private val stop: AtomicBoolean = AtomicBoolean(false)
    private val start: AtomicBoolean = AtomicBoolean(false)

/*
        Это стансдартный подход, когда нужно разрезать стрим, если окно становится слишком широким
        То бишь, если между двумя элементами временное окно слишком большое, то срабатывает дебаунс.
        Буфер ловит это срабатывание и сбрасывает буфер, отправляя предыдущие значения листом подписчикам
*/
    private val subscription: Disposable = debounceSubject
            .buffer(debounceSubject.debounce(debounceTime.toMillis(), TimeUnit.MILLISECONDS))
            .subscribe {
                barcodeSubject.onNext(it.joinToString(separator = ""))
            }


    override fun detached() {
        logger.info("Scanner is detached.")
        subscription.dispose()
        stop.set(true)
    }

    fun startReading() {
        if (!start.compareAndSet(false, true)) {
            throw IllegalStateException("Already started!")
        }

        thread {
            var iface: UsbInterface? = null
            var pipe: UsbPipe? = null
            try {
                Thread.sleep(1000)
                iface = usbDevice.activeUsbConfiguration.usbInterfaces
                        .first()!!
                        .let { it as UsbInterface }
                        .also { it.claim { true } }

                pipe = iface.usbEndpoints.first()!!
                        .let { it as UsbEndpoint }
                        .usbPipe.also { it.open() }

                while (!stop.get()) {
                    val bytes = ByteArray(8)
                    if (pipe.syncSubmit(bytes) != 8) {
                        throw RuntimeException("SyncSubmit error")
                    }
                    sendBarcode(bytes)
                }
            } catch (e: Exception) {
                logger.error("Error, while doing reading", e)
            } finally {
                try {
                    pipe?.close()
                } catch (e: Throwable) {
                    //Ожидаемая ошибка при отсоединении
                    if (!(e is UsbPlatformException && e.errorCode == 4)) {
                        logger.error("Error, while closing pipe", e)
                    }
                }

                try {
                    iface?.release()
                } catch (e: Throwable) {
                    //Ожидаемая ошибка при отсоединении
                    if (!(e is UsbPlatformException && e.errorCode == 4)) {
                        logger.error("Error, while closing iface", e)
                    }
                }

            }

        }
    }

/*
    final StringBuilder kk = new StringBuilder ( );
    for ( int i = 0; i < this.data.length ( ); i++ ){
        final char x = (char)(( '1' - 0x1e ) + this.data.charAt ( i ));      // странная кодировка?
        if ( x >= '0' && x <= '9' )
            kk.append ( x );
        if ( x == ':' )
            kk.append ( '0' );
    }
    this.data.setLength ( 0 );
    this.data.trimToSize ( );
    return kk.toString ( );
*/


    private fun parseBarcode(data: ByteArray): String {
        val kk = StringBuilder()

        data.forEach { byte ->
            val x = ('1' - 0x1e + Util.unsignedByte(byte).toInt())
            if (x in '0'..'9')
                kk.append(x)
            if (x == ':')
                kk.append('0')
        }
        return kk.toString()
    }

    private fun sendBarcode(bytes: ByteArray) {
        val parseBarcode = parseBarcode(bytes)
        println("parseBarcode >>>>>>>>>>>>>>>>>>>>>>>>>> $parseBarcode")
        debounceSubject.onNext(parseBarcode)
    }

    private companion object {
        val logger = getLogger()
    }
}



/*
package iot.example.devices.basic;

import javax.usb.*;
import iot.example.devices.*;
import iot.example.devices.utils.*;

public final class ScannerDevice extends LibUsbDevice
{
//private static final int TIMEOUT_LONGPOLL = 45 * 1000;  // 45 секунд
  private static final int TIMEOUT_APPEND   = 100;        // 100 милллисекунд на дочитывание

  private final StringBuilder data;
  private final StringBuilder clientUuid;
  private int clientSeqnum;

// сканер на длинном шнурке
  public static ScannerDevice MetrologicScan ( )
  {
    return new ScannerDevice ( null, new BasicResponse ( "Metrologic:" ), VID_METROLOGIC, PID_METROLOGIC );
  }

// сканер штрихкодов FuzzyScan cino
  public static ScannerDevice FuzzyScan ( )
  {
    return new ScannerDevice ( null, new BasicResponse ( "FuzzyScan:" ), VID_FUZZYSCAN, PID_FUZZYSCAN );
  }

// клавиатура беспроводная ( типа попробовать как она работает в качестве сканера )
  public static ScannerDevice Keyboard24G ( )
  {
    return new ScannerDevice ( null, new BasicResponse ( "Keyboard:" ), VID_24G_RF_KEYBOARD, PID_24G_RF_KEYBOARD );
  }

// Сканер беленький китайский
  public static ScannerDevice Shangchen ( )
  {
    return new ScannerDevice ( null, new BasicResponse ( "Shangchen:" ), VID_SHANGCHEN, PID_SHANGCHEN );
  }

  public ScannerDevice ( final BasicRequest request, final BasicResponse response, final int vid, final int pid )
  {
    this ( request, response, vid, pid, 0, 0, 0 );
  }

  public ScannerDevice ( final BasicRequest request, final BasicResponse response, final int vid, final int pid, final int iface, final int ep_in, final int ep_out )
  {
    super ( request, response, DEVICE_SCANNER, vid, pid, iface, ep_in, ep_out );
    this.data = new StringBuilder ( );
    this.clientUuid = new StringBuilder ( );
  }

  private boolean isSameRequest ( final ScannerRequest request )
  {
    synchronized ( this.clientUuid )
    {
      return this.clientUuid.toString ( ).equals ( request.clientUuid );
    }
  }

  private void checkSameClient ( final ScannerRequest request ) throws Throwable
  {
    synchronized ( this.clientUuid )
    {
      xassert ( isSameRequest ( request ), "another login. oldUuid:" + request.clientUuid + " newUuid:" + this.clientUuid.toString ( ), null, ERROR_UNKNOWN );
    }
  }

  private boolean claimDevice ( final ScannerRequest request ) throws Throwable
  {
    synchronized ( this.clientUuid )
    {
      if ( isSameRequest ( request ) ) // реквест от того же клиента, что и в прошлый раз
      { // проверим нумерацию запросов
        xassert ( request.clientSeqnum == this.clientSeqnum + 1, "invalid sequence number. expected: " + ( this.clientSeqnum + 1 ) + " got: " + request.clientSeqnum, null, ERROR_UNKNOWN );
        this.clientSeqnum++;  // всё хорошо, пришел следующий запрос от того же клиента
        return false;
      }
      else // иначе - это новая сессия или новый клиент
      {
        xassert ( request.clientSeqnum == 0, "invalid sequence number. expected: 0 got: " + request.clientSeqnum + " uuid:" + this.clientUuid.toString ( ) + " client:" + request.clientUuid, null, ERROR_UNKNOWN );
        this.clientUuid.setLength ( 0 );
        this.clientUuid.append ( request.clientUuid );
        this.clientSeqnum = 0;
        return true;
      }
    }
  }

  private String getBarcode ( final ScannerRequest request ) throws Throwable
  {
    synchronized ( this.clientUuid )
    {
      checkSameClient ( request );
      final StringBuilder kk = new StringBuilder ( );
      for ( int i = 0; i < this.data.length ( ); i++ )
      {
        final char x = (char)(( '1' - 0x1e ) + this.data.charAt ( i ));      // странная кодировка?
        if ( x >= '0' && x <= '9' )
          kk.append ( x );
        if ( x == ':' )
          kk.append ( '0' );
      }
      this.data.setLength ( 0 );
      this.data.trimToSize ( );
      return kk.toString ( );
    }
  }

  public final String readBarcode ( final ScannerRequest request ) throws Throwable
  {
    final boolean reset = claimDevice ( request );
    synchronized ( this.data )
    {
      synchronized ( this.clientUuid )
      {
        checkSameClient ( request );
        if ( reset )
          this.data.setLength ( 0 );
      }
      final long startTime = System.currentTimeMillis ( );
      final long timeoutLongPoll = ( request.timeout - 5 ) * 1000;
      int len;
      do
      {
        len = this.data.length ( );
        Thread.yield ( );
        this.data.wait ( TIMEOUT_APPEND );
        checkSameClient ( request );
        if ( len < this.data.length ( ) )
          continue;
        if ( this.data.length ( ) > 0 )
          break;
        if ( System.currentTimeMillis ( ) - startTime >= timeoutLongPoll )
          break;
      } while ( true );
      return getBarcode ( request );
    }
  }

    private void store(final byte[] buf) {
        synchronized (this.data) {
            boolean n = false;
            for (int i = 0; i < buf.length; i++) {
//      System.out.print ( "0x" + Util.hexByte ( buf [i] ) + " " );
                if (buf[i] != 0) {
                    this.data.append((char) (buf[i] & 0xFF));
                    n = true;
                }
            }
            if (data.length() > 100) {
                data.setLength(0); // не даем переполняться буферу, если никто его не очищает
                data.trimToSize();
                n = false;
            }
            if (n) {
                this.data.notify();
            }
        }
    }

  @Override
  public final Object run ( final String rollbarAccountInfo, final String method, final BasicExecutor exec ) throws Throwable
  {
    UsbInterface xiface = null;
    UsbPipe pipe = null;
    try
    {
      Thread.sleep ( 1000 );
      (xiface = (UsbInterface)(findDevice ( null )).getActiveUsbConfiguration ( ).getUsbInterfaces ( ).get ( this.iface )).claim ( this );
      (pipe = ((UsbEndpoint)xiface.getUsbEndpoints ( ).get ( this.ep_in )).getUsbPipe ( )).open ( );
      for ( final byte[] data = new byte[8]; !this.stop; )
      {
        xassert ( pipe.syncSubmit ( data ) == 8, "syncSubmit error", null, ERROR_NO_DEVICE );
        store ( data );
      }
    }
    catch ( final Throwable x )
    {
//    Rollbar.warn ( x, rollbarAccountInfo ); // не считаем ошибкой вытыкание сканера и связанные с этим эксепшены
      x.printStackTrace ( );
    }
    finally
    {
      try { pipe.close ( ); } catch ( final Throwable x ) { }
      try { xiface.release ( ); } catch ( final Throwable x ) { }
      return null;
    }
  }

  @Override
  protected final void write ( final byte[] data ) throws Throwable
  {
    xassert ( false, "write not implemented", null, ERROR_NOT_IMPLEMENTED );
  }

  @Override
  protected final byte[] read ( ) throws Throwable
  {
    xassert ( false, "read not implemented", null, ERROR_NOT_IMPLEMENTED );
    return null;
  }
}

 */