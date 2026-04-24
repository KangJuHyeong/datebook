package app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "export_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_export_items_request_daily_question",
                columnNames = {"export_request_id", "daily_question_id"})
})
public class ExportItem extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "export_request_id", nullable = false)
    private ExportRequest exportRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_question_id", nullable = false)
    private DailyQuestion dailyQuestion;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ExportItem() {
    }

    public ExportItem(ExportRequest exportRequest, DailyQuestion dailyQuestion, int sortOrder) {
        this.exportRequest = exportRequest;
        this.dailyQuestion = dailyQuestion;
        this.sortOrder = sortOrder;
    }

    public ExportRequest getExportRequest() {
        return exportRequest;
    }

    public DailyQuestion getDailyQuestion() {
        return dailyQuestion;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
