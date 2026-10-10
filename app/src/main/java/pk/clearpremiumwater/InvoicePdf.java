package pk.clearpremiumwater;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.database.Cursor;
import android.net.Uri;
import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;

public final class InvoicePdf {
    private InvoicePdf() {
    }

    private static String money(long n) {
        return "Rs. " + NumberFormat.getNumberInstance(Locale.US).format(n);
    }

    public static File create(Context ctx, Db db, long invoiceId) throws IOException {
        PdfDocument pdf = new PdfDocument();
        Paint p = new Paint(3);
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(info);
        Canvas c = page.getCanvas();
        int red = Color.rgb(190, 35, 45), navy = Color.rgb(16, 36, 58);
        int y = 0;
        p.setColor(navy);
        c.drawRect(0, 0, 595, 130, p);
        p.setColor(Color.WHITE);
        p.setTextSize(25);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        c.drawText("CLEAR PREMIUM WATER", 35, 48, p);
        p.setTextSize(11);
        p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        c.drawText("Faram Rahimabad, Mingora, Swat", 35, 72, p);
        c.drawText("Contact: 03425214489", 35, 91, p);
        p.setTextSize(20);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("INVOICE", 455, 48, p);
        long customer = 0, total = 0, previous = 0;
        String date = "", name = "", phone = "", address = "";
        try (Cursor q = db.getReadableDatabase().rawQuery(
                "SELECT i.customer_id,i.invoice_date,i.invoice_total,i.previous_balance,c.name,c.phone,c.address FROM invoices i JOIN customers c ON c.id=i.customer_id WHERE i.id=?",
                new String[] { "" + invoiceId })) {
            if (q.moveToFirst()) {
                customer = q.getLong(0);
                date = q.getString(1);
                total = q.getLong(2);
                previous = q.getLong(3);
                name = q.getString(4);
                phone = q.getString(5);
                address = q.getString(6);
            }
        }
        p.setColor(navy);
        p.setTextSize(12);
        c.drawText("Invoice #: CPW-" + String.format(Locale.US, "%05d", invoiceId), 35, 160, p);
        c.drawText("Date: " + date, 370, 160, p);
        c.drawText("Bill To: " + name, 35, 190, p);
        c.drawText("Phone: " + (phone == null ? "" : phone), 35, 210, p);
        c.drawText("Address: " + (address == null ? "" : address), 35, 230, p);
        p.setColor(red);
        c.drawRect(35, 255, 560, 283, p);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("Product", 45, 273, p);
        c.drawText("Qty", 325, 273, p);
        c.drawText("Rate", 390, 273, p);
        c.drawText("Amount", 475, 273, p);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        p.setColor(navy);
        y = 305;
        try (Cursor q = db.getReadableDatabase().rawQuery(
                "SELECT product_name,quantity,rate,line_total FROM invoice_items WHERE invoice_id=?",
                new String[] { "" + invoiceId })) {
            while (q.moveToNext() && y < 650) {
                c.drawText(q.getString(0), 45, y, p);
                c.drawText(q.getString(1), 325, y, p);
                c.drawText(money(q.getLong(2)), 390, y, p);
                c.drawText(money(q.getLong(3)), 475, y, p);
                y += 25;
            }
        }
        long paid = 0;
        try (Cursor q = db.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE invoice_id=?", new String[] { "" + invoiceId })) {
            if (q.moveToFirst())
                paid = q.getLong(0);
        }
        long due = Math.max(0, previous + total - paid);
        p.setColor(Color.LTGRAY);
        c.drawLine(300, y + 10, 560, y + 10, p);
        p.setColor(navy);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("Current invoice: " + money(total), 330, y + 38, p);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        c.drawText("Previous balance: " + money(previous), 330, y + 61, p);
        c.drawText("Payment received: " + money(paid), 330, y + 84, p);
        p.setColor(red);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(15);
        c.drawText("Remaining balance: " + money(due), 330, y + 112, p);
        p.setColor(navy);
        p.setTextSize(12);
        c.drawText("Thank you for choosing CLEAR PREMIUM WATER!", 35, 790, p);
        c.drawText("Please keep this invoice for your records.", 35, 810, p);
        pdf.finishPage(page);
        File dir = new File(ctx.getCacheDir(), "invoices");
        if (!dir.exists())
            dir.mkdirs();
        File out = new File(dir, "CPW-" + String.format(Locale.US, "%05d", invoiceId) + ".pdf");
        try (FileOutputStream os = new FileOutputStream(out)) {
            pdf.writeTo(os);
        } finally {
            pdf.close();
        }
        return out;
    }
}
