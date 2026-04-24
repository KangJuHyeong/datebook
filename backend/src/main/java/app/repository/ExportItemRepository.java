package app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.ExportItem;

public interface ExportItemRepository extends JpaRepository<ExportItem, Long> {

    List<ExportItem> findAllByExportRequest_IdOrderBySortOrderAsc(Long exportRequestId);
}
