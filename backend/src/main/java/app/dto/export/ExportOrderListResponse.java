package app.dto.export;

import java.util.List;

public record ExportOrderListResponse(
        List<ExportOrderSummaryResponse> orders
) {
}
