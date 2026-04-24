package app.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import app.domain.Couple;
import app.domain.DailyQuestion;
import app.domain.Question;
import app.domain.User;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private DailyQuestionRepository dailyQuestionRepository;

    @Test
    @DisplayName("users email unique 제약을 검증한다")
    void enforcesUniqueEmailConstraint() {
        userRepository.saveAndFlush(new User("one@example.com", "hash-1", "하나"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("one@example.com", "hash-2", "둘")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 커플과 날짜의 daily question은 하나만 허용한다")
    void enforcesUniqueDailyQuestionPerCoupleAndDate() {
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        Question firstQuestion = questionRepository.saveAndFlush(new Question("질문 1", true, 1));
        Question secondQuestion = questionRepository.saveAndFlush(new Question("질문 2", true, 2));
        LocalDate today = LocalDate.of(2026, 4, 24);

        dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, firstQuestion, today));

        assertThatThrownBy(() -> dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, secondQuestion, today)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("커플과 날짜로 오늘 질문을 조회한다")
    void findsDailyQuestionByCoupleAndDate() {
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        LocalDate today = LocalDate.of(2026, 4, 24);
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, question, today));

        assertThat(dailyQuestionRepository.findByCouple_IdAndQuestionDate(couple.getId(), today))
                .contains(dailyQuestion);
    }

    @Test
    @DisplayName("active 질문은 sort_order, id 오름차순으로 조회한다")
    void findsActiveQuestionsInExpectedOrder() {
        Question third = questionRepository.saveAndFlush(new Question("세 번째", true, 3));
        Question inactive = questionRepository.saveAndFlush(new Question("비활성", false, 1));
        Question first = questionRepository.saveAndFlush(new Question("첫 번째", true, 1));
        Question tiedThird = questionRepository.saveAndFlush(new Question("세 번째-2", true, 3));

        assertThat(questionRepository.findByActiveTrueOrderBySortOrderAscIdAsc())
                .extracting(Question::getId)
                .containsExactly(first.getId(), third.getId(), tiedThird.getId());
        assertThat(inactive.isActive()).isFalse();
    }
}
