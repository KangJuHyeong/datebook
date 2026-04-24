package app.service;

import java.time.LocalDate;
import java.util.List;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.common.time.AppTimeProvider;
import app.domain.Answer;
import app.domain.AnswerState;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.PartnerAnswerStatus;
import app.domain.Question;
import app.dto.question.PartnerAnswerResponse;
import app.dto.question.TodayAnswerResponse;
import app.dto.question.TodayQuestionResponse;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.QuestionRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DailyQuestionService {

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AppTimeProvider appTimeProvider;

    public DailyQuestionService(
            UserRepository userRepository,
            CoupleMemberRepository coupleMemberRepository,
            DailyQuestionRepository dailyQuestionRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            AppTimeProvider appTimeProvider
    ) {
        this.userRepository = userRepository;
        this.coupleMemberRepository = coupleMemberRepository;
        this.dailyQuestionRepository = dailyQuestionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.appTimeProvider = appTimeProvider;
    }

    @Transactional
    public TodayQuestionResponse getTodayQuestion(HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        LocalDate today = appTimeProvider.todaySeoul();
        Long coupleId = coupleMember.getCouple().getId();

        DailyQuestion dailyQuestion = dailyQuestionRepository.findByCouple_IdAndQuestionDate(coupleId, today)
                .orElseGet(() -> createDailyQuestion(coupleMember.getCouple(), today));

        return buildResponse(dailyQuestion, coupleMember.getUser().getId());
    }

    private DailyQuestion createDailyQuestion(Couple couple, LocalDate today) {
        List<Question> activeQuestions = questionRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        if (activeQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFIGURATION_ERROR);
        }

        long existingCount = dailyQuestionRepository.countByCouple_Id(couple.getId());
        Question selectedQuestion = activeQuestions.get((int) (existingCount % activeQuestions.size()));

        try {
            return dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, selectedQuestion, today));
        } catch (DataIntegrityViolationException exception) {
            return dailyQuestionRepository.findByCouple_IdAndQuestionDate(couple.getId(), today)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DAILY_QUESTION_CONFLICT));
        }
    }

    private TodayQuestionResponse buildResponse(DailyQuestion dailyQuestion, Long currentUserId) {
        List<Answer> answers = answerRepository.findAllByDailyQuestion_Id(dailyQuestion.getId());
        Answer myAnswer = answers.stream()
                .filter(answer -> answer.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        Answer partnerAnswer = answers.stream()
                .filter(answer -> !answer.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        AnswerState answerState = resolveAnswerState(myAnswer, partnerAnswer);

        return new TodayQuestionResponse(
                dailyQuestion.getId(),
                dailyQuestion.getQuestionDate(),
                answerState,
                dailyQuestion.getQuestion().getContent(),
                toTodayAnswerResponse(myAnswer),
                toPartnerAnswerResponse(answerState, partnerAnswer),
                answerState == AnswerState.BOTH_ANSWERED
        );
    }

    private AnswerState resolveAnswerState(Answer myAnswer, Answer partnerAnswer) {
        if (myAnswer != null && partnerAnswer != null) {
            return AnswerState.BOTH_ANSWERED;
        }
        if (myAnswer != null) {
            return AnswerState.MY_ANSWERED_PARTNER_WAITING;
        }
        if (partnerAnswer != null) {
            return AnswerState.PARTNER_ANSWERED_ME_WAITING;
        }
        return AnswerState.NOT_ANSWERED;
    }

    private TodayAnswerResponse toTodayAnswerResponse(Answer answer) {
        if (answer == null) {
            return null;
        }
        return new TodayAnswerResponse(answer.getId(), answer.getContent(), answer.getUpdatedAt());
    }

    private PartnerAnswerResponse toPartnerAnswerResponse(AnswerState answerState, Answer partnerAnswer) {
        if (answerState == AnswerState.BOTH_ANSWERED && partnerAnswer != null) {
            return new PartnerAnswerResponse(
                    PartnerAnswerStatus.REVEALED,
                    partnerAnswer.getId(),
                    partnerAnswer.getContent(),
                    partnerAnswer.getUpdatedAt()
            );
        }
        if (answerState == AnswerState.PARTNER_ANSWERED_ME_WAITING) {
            return new PartnerAnswerResponse(PartnerAnswerStatus.ANSWERED_LOCKED, null, null, null);
        }
        return new PartnerAnswerResponse(PartnerAnswerStatus.LOCKED, null, null, null);
    }

    private CoupleMember getAuthenticatedCoupleMember(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));

        return coupleMemberRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }
}
