package common.payment;

/**
 * Immutable Payment record — passed between Gateway, DAO, and Services.
 */
public class PaymentModel {

    // ── Fields ────────────────────────────────────────────────────────────────
    private final int    paymentId;
    private final String bookingRef;
    private final String systemType;       // RAILWAY | FLIGHT | BUS
    private final double amount;
    private final String paymentMethod;    // CASH | UPI
    private final String paymentStatus;    // SUCCESS | FAILED | REFUNDED
    private final String transactionId;
    private final String paidAt;

    // ── Constructor ───────────────────────────────────────────────────────────
    public PaymentModel(int paymentId, String bookingRef, String systemType,
                        double amount, String paymentMethod,
                        String paymentStatus, String transactionId, String paidAt) {
        this.paymentId     = paymentId;
        this.bookingRef    = bookingRef;
        this.systemType    = systemType;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.paidAt        = paidAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int    getPaymentId()     { return paymentId; }
    public String getBookingRef()    { return bookingRef; }
    public String getSystemType()    { return systemType; }
    public double getAmount()        { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getTransactionId() { return transactionId; }
    public String getPaidAt()        { return paidAt; }

    @Override
    public String toString() {
        return "PaymentModel{ref=" + bookingRef + ", system=" + systemType
             + ", amount=" + amount + ", method=" + paymentMethod
             + ", status=" + paymentStatus + ", txn=" + transactionId + "}";
    }
}