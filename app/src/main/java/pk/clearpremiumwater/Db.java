package pk.clearpremiumwater;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;

public class Db extends SQLiteOpenHelper {
    public static final String NAME="clear_premium_water.db";
    public Db(Context c){super(c,NAME,null,1);}
    @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT,active INTEGER NOT NULL DEFAULT 1)");
        d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,active INTEGER NOT NULL DEFAULT 1)");
        d.execSQL("CREATE TABLE customer_rates(customer_id INTEGER NOT NULL,product_id INTEGER NOT NULL,rate INTEGER NOT NULL CHECK(rate>=0),PRIMARY KEY(customer_id,product_id),FOREIGN KEY(customer_id) REFERENCES customers(id),FOREIGN KEY(product_id) REFERENCES products(id))");
        d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,invoice_date TEXT NOT NULL,invoice_total INTEGER NOT NULL,previous_balance INTEGER NOT NULL,received_at_creation INTEGER NOT NULL DEFAULT 0,note TEXT,FOREIGN KEY(customer_id) REFERENCES customers(id))");
        d.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER NOT NULL,product_id INTEGER NOT NULL,product_name TEXT NOT NULL,quantity INTEGER NOT NULL,rate INTEGER NOT NULL,line_total INTEGER NOT NULL,FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE)");
        d.execSQL("CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,invoice_id INTEGER,paid_date TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),note TEXT,FOREIGN KEY(customer_id) REFERENCES customers(id),FOREIGN KEY(invoice_id) REFERENCES invoices(id))");
        String[] products={"19 Litre Water Can","1.5 Litre Water Bottle","500 ml Bottle","330 ml Bottle","Distilled Water"};
        for(String p:products){ContentValues v=new ContentValues();v.put("name",p);d.insert("products",null,v);}
    }
    @Override public void onUpgrade(SQLiteDatabase d,int oldV,int newV) { /* Future schema changes must use explicit migrations. */ }
    public long addCustomer(String name,String phone,String address){ContentValues v=new ContentValues();v.put("name",name.trim());v.put("phone",phone);v.put("address",address);return getWritableDatabase().insertOrThrow("customers",null,v);}
    public void updateCustomer(long id,String name,String phone,String address){ContentValues v=new ContentValues();v.put("name",name.trim());v.put("phone",phone);v.put("address",address);getWritableDatabase().update("customers",v,"id=?",new String[]{""+id});}
    public long addProduct(String name){ContentValues v=new ContentValues();v.put("name",name.trim());return getWritableDatabase().insertOrThrow("products",null,v);}
    public void setRate(long customer,long product,long rate){ContentValues v=new ContentValues();v.put("customer_id",customer);v.put("product_id",product);v.put("rate",rate);getWritableDatabase().insertWithOnConflict("customer_rates",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public long rate(long customer,long product){try(Cursor c=getReadableDatabase().rawQuery("SELECT rate FROM customer_rates WHERE customer_id=? AND product_id=?",new String[]{""+customer,""+product})){return c.moveToFirst()?c.getLong(0):0;}}
    public long balance(long customer){String q="SELECT COALESCE((SELECT SUM(invoice_total) FROM invoices WHERE customer_id=?),0)+COALESCE((SELECT SUM(amount) FROM payments WHERE customer_id=?),0)*0-COALESCE((SELECT SUM(amount) FROM payments WHERE customer_id=?),0)";try(Cursor c=getReadableDatabase().rawQuery(q,new String[]{""+customer,""+customer,""+customer})){return c.moveToFirst()?c.getLong(0):0;}}
    public long outstanding(long customer){try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE((SELECT SUM(invoice_total) FROM invoices WHERE customer_id=?),0)-COALESCE((SELECT SUM(amount) FROM payments WHERE customer_id=?),0)",new String[]{""+customer,""+customer})){return c.moveToFirst()?Math.max(0,c.getLong(0)):0;}}
    public long createInvoice(long customer,String date,java.util.List<InvoiceLine> lines,long payment,String note){
        if(lines.isEmpty())throw new IllegalArgumentException("Add at least one product");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            long prev=outstandingInside(d,customer),total=0;
            for(InvoiceLine l:lines){if(l.quantity<=0)throw new IllegalArgumentException("Quantity must be greater than zero");total=Math.addExact(total,BillingMath.lineTotal(l.rate,l.quantity));}
            ContentValues iv=new ContentValues();iv.put("customer_id",customer);iv.put("invoice_date",date);iv.put("invoice_total",total);iv.put("previous_balance",prev);iv.put("received_at_creation",0);iv.put("note",note);long invoice=d.insertOrThrow("invoices",null,iv);
            for(InvoiceLine l:lines){ContentValues v=new ContentValues();v.put("invoice_id",invoice);v.put("product_id",l.productId);v.put("product_name",l.productName);v.put("quantity",l.quantity);v.put("rate",l.rate);v.put("line_total",BillingMath.lineTotal(l.rate,l.quantity));d.insertOrThrow("invoice_items",null,v);}
            if(payment>0){ContentValues p=new ContentValues();p.put("customer_id",customer);p.put("invoice_id",invoice);p.put("paid_date",date);p.put("amount",payment);p.put("note","Payment at invoice creation");d.insertOrThrow("payments",null,p);}
            d.setTransactionSuccessful();return invoice;
        }finally{d.endTransaction();}
    }
    private long outstandingInside(SQLiteDatabase d,long customer){try(Cursor c=d.rawQuery("SELECT COALESCE((SELECT SUM(invoice_total) FROM invoices WHERE customer_id=?),0)-COALESCE((SELECT SUM(amount) FROM payments WHERE customer_id=?),0)",new String[]{""+customer,""+customer})){return c.moveToFirst()?Math.max(0,c.getLong(0)):0;}}
    public long addPayment(long customer,Long invoice,String date,long amount,String note){if(amount<=0)throw new IllegalArgumentException("Payment must be greater than zero");ContentValues p=new ContentValues();p.put("customer_id",customer);if(invoice!=null)p.put("invoice_id",invoice);p.put("paid_date",date);p.put("amount",amount);p.put("note",note);return getWritableDatabase().insertOrThrow("payments",null,p);}
    public static class InvoiceLine {public long productId,rate,quantity;public String productName;public InvoiceLine(long id,String name,long qty,long price){productId=id;productName=name;quantity=qty;rate=price;}}
}
