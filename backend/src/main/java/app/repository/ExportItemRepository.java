package app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.domain.ExportItem;

public interface ExportItemRepository extends JpaRepository<ExportItem, Long> {

    List<ExportItem> findAllByExportRequest_IdOrderBySortOrderAsc(Long exportRequestId);

    int countByExportRequest_Id(Long exportRequestId);

    @Query("""
            select item.exportRequest.id as exportRequestId, count(item) as itemCount
            from ExportItem item
            where item.exportRequest.id in :exportRequestIds
            group by item.exportRequest.id
            """)
    List<ExportItemCount> countByExportRequestIds(@Param("exportRequestIds") List<Long> exportRequestIds);

    void deleteAllByExportRequest_Id(Long exportRequestId);

    interface ExportItemCount {
        Long getExportRequestId();

        Long getItemCount();
    }
}
