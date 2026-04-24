package app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.CoupleMember;

public interface CoupleMemberRepository extends JpaRepository<CoupleMember, Long> {

    boolean existsByUser_Id(Long userId);

    long countByCouple_Id(Long coupleId);

    Optional<CoupleMember> findByUser_Id(Long userId);

    List<CoupleMember> findAllByCouple_Id(Long coupleId);

    List<CoupleMember> findAllByCouple_IdOrderByJoinedAtAscIdAsc(Long coupleId);
}
