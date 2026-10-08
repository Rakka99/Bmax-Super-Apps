package id.bmax.app.feature.invoice

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import id.bmax.app.feature.customer.CustomerDto

object InvoicePrinter {
    fun print(context: Context, customer: CustomerDto, invoice: InvoiceDisplayDto) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.settings.domStorageEnabled = false

        val jobName = "Invoice-${invoice.idpel}-${invoice.billingPeriod ?: invoice.period}"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val adapter: PrintDocumentAdapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build(),
                )
            }
        }

        webView.loadDataWithBaseURL(
            null,
            buildHtml(customer, invoice),
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun buildHtml(customer: CustomerDto, invoice: InvoiceDisplayDto): String {
        val standMeter = invoice.standMeter?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
        val admin = invoice.adminPln
        val penalty = invoice.penalty
        val adminRow = if (admin > 0) {
            "<tr><td>Admin PLN</td><td class='money'>${money(admin)}</td></tr>"
        } else ""
        val penaltyRow = if (penalty > 0) {
            "<tr><td>Denda</td><td class='money'>${money(penalty)}</td></tr>"
        } else ""

        return """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <style>
                @page { size: A4; margin: 18mm; }
                body { font-family: sans-serif; color:#101828; }
                .header { border-bottom:3px solid #1976D2; padding-bottom:12px; margin-bottom:18px; }
                .brand { font-size:22px; font-weight:700; color:#1976D2; }
                .sub { font-size:12px; color:#667085; }
                h1 { font-size:20px; margin:18px 0 10px; }
                table { width:100%; border-collapse:collapse; }
                td { padding:8px 4px; border-bottom:1px solid #e4e7ec; vertical-align:top; }
                td:first-child { width:34%; color:#667085; }
                .money { text-align:right; font-weight:600; color:#101828; }
                .stand { font-family:monospace; letter-spacing:1px; font-weight:700; }
                .total { font-size:18px; font-weight:800; border-top:2px solid #101828; }
                .note { margin-top:22px; font-size:11px; color:#667085; }
              </style>
            </head>
            <body>
              <div class="header">
                <div class="brand">PLN Electricity Services</div>
                <div class="sub">PLN UP3 SUMEDANG • Bmax Super Apps</div>
              </div>
              <h1>INVOICE PEMBAYARAN LISTRIK</h1>
              <table>
                <tr><td>ID Pelanggan</td><td>${escape(invoice.idpel)}</td></tr>
                <tr><td>Nama Pelanggan</td><td>${escape(invoice.customerName.ifBlank { customer.name })}</td></tr>
                <tr><td>Tarif / Daya</td><td>${escape(customer.tariff ?: "-")} / ${customer.powerVa ?: 0} VA</td></tr>
                <tr><td>Periode</td><td>${escape(invoice.period)}</td></tr>
                <tr><td>Kategori</td><td>${escape(invoice.category)}</td></tr>
                <tr><td>Status</td><td>${escape(invoice.status)}</td></tr>
                <tr><td>Stand Meter</td><td class="stand">${escape(standMeter)}</td></tr>
              </table>
              <h1>RINCIAN TAGIHAN</h1>
              <table>
                <tr><td>RPTAG PLN</td><td class="money">${money(invoice.rptagPln)}</td></tr>
                $adminRow
                $penaltyRow
                <tr><td class="total">TOTAL</td><td class="money total">${money(invoice.invoiceTotal)}</td></tr>
              </table>
              <div class="note">Stand meter berasal dari data inquiry PLN Postpaid PituCode. Nilai ini hanya informasi meter dan tidak mengubah RPTAG, admin, denda, atau status tagihan.</div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun money(value: Long): String =
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(value)
}
