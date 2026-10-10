package pk.clearpremiumwater;

/** Pure billing calculations; all money is stored as integer paisa/whole PKR in this app. */
public final class BillingMath {
    private BillingMath() {}
    public static long lineTotal(long unitRate, long quantity) {
        if (unitRate < 0 || quantity < 0) throw new IllegalArgumentException("Rate and quantity cannot be negative");
        return Math.multiplyExact(unitRate, quantity);
    }
    public static long totalDue(long previousBalance, long invoiceTotal) {
        if (previousBalance < 0 || invoiceTotal < 0) throw new IllegalArgumentException("Amounts cannot be negative");
        return Math.addExact(previousBalance, invoiceTotal);
    }
    public static long remaining(long previousBalance, long invoiceTotal, long payment) {
        if (payment < 0) throw new IllegalArgumentException("Payment cannot be negative");
        return Math.max(0L, Math.subtractExact(totalDue(previousBalance, invoiceTotal), payment));
    }
}
