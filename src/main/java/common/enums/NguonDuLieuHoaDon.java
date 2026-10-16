package common.enums;

/** Describes where invoice display names came from; legacy history is never inferred. */
public enum NguonDuLieuHoaDon {
    /** Current catalog/party data for drafts and canceled invoices. */
    DU_LIEU_HIEN_TAI,
    /** Complete names captured atomically when a new invoice was paid. */
    SNAPSHOT_THANH_TOAN,
    /** A paid legacy/partial record uses whatever current values remain available. */
    FALLBACK_DU_LIEU_HIEN_TAI
}
