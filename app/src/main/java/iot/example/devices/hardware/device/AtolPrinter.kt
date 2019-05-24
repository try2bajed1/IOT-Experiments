package iot.example.devices.device

import android.hardware.usb.UsbDevice
import org.funktionale.either.Either
import iot.example.devices.PrinterException
import javax.usb.UsbDevice
import iot.example.devices.legacy.AtolPrinter as LegacyPrinter
import iot.example.devices.legacy.PrintingResult as LegacyPrintingResult

open class AtolPrinter(vid: Int, pid: Int, usbDevice: UsbDevice) : FiscalPrinter(vid, pid, usbDevice, "Атол") {

    private val legacyPrinter = LegacyPrinter(vid, pid)
    override val printerIndex: Int = legacyPrinter.modelIndex
    override val fnNumber: String? = legacyPrinter.fnNumber
    override val factoryKktNumber: String? = legacyPrinter.znNumber
    override val registrationNumber: String? = legacyPrinter.qqNumber
    override val receiptWidth: Int = legacyPrinter.receiptWidth
    val atolPrinterSerial: String? = legacyPrinter.atolSerial

    //Легаси
    @Throws(PrinterException::class)
    override fun v1Print(printingRequest: PrintingRequest?, method: String): V1PrintingResult {
        return when (method) {
            "open_session" ->
                legacyPrinter.openSession()
            "purchase" ->
                legacyPrinter.printSaleCheck(printingRequest)
            "refund" ->
                legacyPrinter.printRefundCheck(printingRequest!!)
            "z_report" ->
                legacyPrinter.reportZ(printingRequest?.disableZ ?: false, printingRequest?.processAsMoeDelo ?: false)
            "x_report" ->
                legacyPrinter.reportX()
            "test_check" ->
                legacyPrinter.printTestCheck()
            "setup" ->
                legacyPrinter.setupValues(printingRequest)
            "fn_report" ->
                legacyPrinter.reportFn()
            "x_hourly" ->
                legacyPrinter.reportHourly()
            "cash_income" ->
                legacyPrinter.cashIncome(printingRequest)
            "cash_outcome" ->
                legacyPrinter.cashOutcome(printingRequest)
            "copy" ->
                legacyPrinter.printCheckCopy(printingRequest)
            "correction" ->
                legacyPrinter.printCheckCorrectionOld(printingRequest)
            "cor_return" ->
                legacyPrinter.printCheckRefundCorrectionOld(printingRequest)
            "slip" ->
                legacyPrinter.printCheckSlip(printingRequest!!.text!!)
            else ->
                notImplemented(method)
        }
                .let { it: LegacyPrintingResult ->
                    if (it.isOK)
                        V1PrintingResult(it.result)
                    else
                        throw PrinterException(message = it.error!!.description,
                                code = it.error.code,
                                data = it.result,
                                recoverable = it.error.isRecoverable,
                                deviceId = vidPidKey)
                }

    }


    override fun purchase(purchaseRequest: PurchaseRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.printSaleCheck(purchaseRequest.toLegacyPrintRequest()).toFiscalPrintingResult()


    override fun refund(purchaseRequest: RefundRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.printRefundCheck(purchaseRequest.toLegacyPrintRequest()).toFiscalPrintingResult()


    override fun openRetailShift(): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.openSession().toFiscalPrintingResult()


    override fun zReport(zReportRequest: ZReportRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.reportZ(!zReportRequest.largeReport,false).toFiscalPrintingResult()


    override fun xReport(): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.reportX().toFiscalPrintingResult()


    override fun hourlyReport(): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.reportHourly().toFiscalPrintingResult()


    override fun accountingStateReport(): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.reportFn().toFiscalPrintingResult()


    override fun text(text: TextPrintingRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.printCheckSlip(text.text).toFiscalPrintingResult()


    override fun testReceipt(): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.printTestCheck().toFiscalPrintingResult()


    override fun cashIncome(cashOperationRequest: CashOperationRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.cashIncome(cashOperationRequest.toIncomeLegacyRequest()).toFiscalPrintingResult()


    override fun cashOutcome(cashOperationRequest: CashOperationRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.cashOutcome(cashOperationRequest.toIncomeLegacyRequest()).toFiscalPrintingResult()


    override fun correction(cashOperationRequest: CorrectionRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.correctionIncome(cashOperationRequest).toFiscalPrintingResult()


    override fun correctionRefund(cashOperationRequest: CorrectionRequest.Data): Either<FiscalPrinterError, FiscalPrintingResult> =
            legacyPrinter.correctionOutcome(cashOperationRequest).toFiscalPrintingResult()


    private fun notImplemented(name: String): Nothing =
            throw PrinterException("Этот метод ($name) не реализован в данный момент.", ERROR_NOT_IMPLEMENTED,
                    data = null, recoverable = true, deviceId = vidPidKey)

    override fun detached() {
        legacyPrinter.detach()
    }

    private fun LegacyPrintingResult.toFiscalPrintingResult() =
            if (this.isOK)
                FiscalPrintingResult(this.result).toRight<FiscalPrinterError, FiscalPrintingResult>()
            else
                FiscalPrinterError(message = this.error!!.description,
                        code = this.error.code,
                        data = this.result,
                        recoverable = this.error.isRecoverable,
                        deviceId = vidPidKey).toLeft()


    private fun PurchaseRequest.Data.toLegacyPrintRequest() =
            PrintingRequest(entered = this.amount,
                    discount = this.discount,
                    method = this.methodType.id,
                    casher = this.cashierName,
                    email = this.clientAddress,
                    ordernum = this.orderNumber,
                    electron = this.digitalReceipt,
                    lines = this.receiptLines.map { it.toLegacyLine() })

    private fun RefundRequest.Data.toLegacyPrintRequest() =
            PrintingRequest(entered = this.amount,
                    discount = this.discount,
                    method = this.methodType.id,
                    casher = this.cashierName,
                    email = this.clientAddress,
                    ordernum = this.orderNumber,
                    electron = this.digitalReceipt,
                    lines = this.receiptLines.map { it.toLegacyLine() },
                    autoIncome = this.autoIncome)

    private fun ReceiptLine.toLegacyLine(): CheckLine =
            CheckLine(title = this.title,
                    price = this.price,
                    quantity = this.quantity,
                    linePrice = this.linePrice,
                    discount = this.discount,
                    tax = this.tax.code,
                    fiscalProductType = this.fiscalProductType,
                    paymentCase = this.paymentCase)

    private fun CashOperationRequest.Data.toIncomeLegacyRequest() =
            PrintingRequest(entered = this.amount,
                    casher = this.cashierName)


    private fun String.filterDigets() =
            this.filter { it in '0'..'9' }
}

