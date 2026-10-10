package pk.clearpremiumwater;

public final class BillingMathTest {
    public static void main(String[] args) {
        check(BillingMath.lineTotal(200, 5) == 1000, "line total");
        check(BillingMath.totalDue(2000, 5000) == 7000, "previous balance plus invoice");
        check(BillingMath.remaining(2000, 5000, 3000) == 4000, "partial payment");
        check(BillingMath.remaining(10000, 0, 4000) == 6000, "payment on existing debt");
        check(BillingMath.remaining(1000, 500, 9000) == 0, "overpayment floor");
        boolean rejected=false; try { BillingMath.lineTotal(-1, 2); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected, "reject negative rate");
        System.out.println("All BillingMath tests passed.");
    }
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
}
