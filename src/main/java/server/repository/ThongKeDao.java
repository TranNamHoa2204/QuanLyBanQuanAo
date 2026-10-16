package server.repository;

import java.time.LocalDateTime;
import java.util.List;

/** Truy vấn nguồn thống kê; danh sách được giới hạn trước khi đưa về service. */
public interface ThongKeDao {
    List<Object[]> paidInvoiceTotals(LocalDateTime tuLuc, LocalDateTime truocLuc, int gioiHan);
    List<Object[]> paidInvoiceLines(LocalDateTime tuLuc, LocalDateTime truocLuc, int gioiHan);
    long countPaidWithoutPaymentTime();
}
