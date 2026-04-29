package app.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.ExportRequest;
import app.domain.ExportStatus;

public interface ExportRequestRepository extends JpaRepository<ExportRequest, Long> {

    List<ExportRequest> findAllByCouple_IdOrderByCreatedAtDesc(Long coupleId);

    List<ExportRequest> findAllByCouple_IdAndStatusInOrderByCreatedAtDesc(Long coupleId, Collection<ExportStatus> statuses);

    Optional<ExportRequest> findByIdAndCouple_Id(Long exportRequestId, Long coupleId);
}
