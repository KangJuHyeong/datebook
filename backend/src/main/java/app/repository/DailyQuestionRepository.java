package app.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.DailyQuestion;

public interface DailyQuestionRepository extends JpaRepository<DailyQuestion, Long> {

    Optional<DailyQuestion> findByCouple_IdAndQuestionDate(Long coupleId, LocalDate questionDate);

    long countByCouple_Id(Long coupleId);

    List<DailyQuestion> findAllByCouple_IdOrderByQuestionDateDesc(Long coupleId);

    List<DailyQuestion> findAllByCouple_IdAndIdIn(Long coupleId, Collection<Long> ids);
}
