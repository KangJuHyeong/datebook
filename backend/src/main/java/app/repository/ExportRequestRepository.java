package app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.ExportRequest;

public interface ExportRequestRepository extends JpaRepository<ExportRequest, Long> {

    List<ExportRequest> findAllByCouple_IdOrderByCreatedAtDesc(Long coupleId);

    Optional<ExportRequest> findByIdAndCouple_Id(Long exportRequestId, Long coupleId);
}
