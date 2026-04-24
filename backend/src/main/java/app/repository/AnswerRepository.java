package app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domain.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByDailyQuestion_IdAndUser_Id(Long dailyQuestionId, Long userId);

    List<Answer> findAllByDailyQuestion_Id(Long dailyQuestionId);
}
