package app.service;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.domain.Answer;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.dto.answer.AnswerResponse;
import app.dto.answer.CreateAnswerRequest;
import app.dto.answer.UpdateAnswerRequest;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnswerService {

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final AnswerRepository answerRepository;

    public AnswerService(
            UserRepository userRepository,
            CoupleMemberRepository coupleMemberRepository,
            DailyQuestionRepository dailyQuestionRepository,
            AnswerRepository answerRepository
    ) {
        this.userRepository = userRepository;
        this.coupleMemberRepository = coupleMemberRepository;
        this.dailyQuestionRepository = dailyQuestionRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public AnswerResponse createOrUpdateAnswer(CreateAnswerRequest request, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        validateContent(request.content());

        DailyQuestion dailyQuestion = dailyQuestionRepository.findById(request.dailyQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!dailyQuestion.getCouple().getId().equals(coupleMember.getCouple().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Long userId = coupleMember.getUser().getId();
        Answer answer = answerRepository.findByDailyQuestion_IdAndUser_Id(dailyQuestion.getId(), userId)
                .map(existingAnswer -> updateExistingAnswer(existingAnswer, request.content()))
                .orElseGet(() -> createAnswer(dailyQuestion, coupleMember, request.content()));

        return toResponse(answer);
    }

    @Transactional
    public AnswerResponse updateAnswer(Long answerId, UpdateAnswerRequest request, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        validateContent(request.content());

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!answer.getDailyQuestion().getCouple().getId().equals(coupleMember.getCouple().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (!answer.getUser().getId().equals(coupleMember.getUser().getId())) {
            throw new BusinessException(ErrorCode.ANSWER_NOT_OWNED);
        }

        answer.updateContent(request.content());
        return toResponse(answerRepository.saveAndFlush(answer));
    }

    private Answer updateExistingAnswer(Answer answer, String content) {
        answer.updateContent(content);
        return answerRepository.saveAndFlush(answer);
    }

    private Answer createAnswer(DailyQuestion dailyQuestion, CoupleMember coupleMember, String content) {
        try {
            return answerRepository.saveAndFlush(new Answer(dailyQuestion, coupleMember.getUser(), content));
        } catch (DataIntegrityViolationException exception) {
            Answer existingAnswer = answerRepository.findByDailyQuestion_IdAndUser_Id(
                            dailyQuestion.getId(),
                            coupleMember.getUser().getId()
                    )
                    .orElseThrow(() -> exception);
            existingAnswer.updateContent(content);
            return answerRepository.saveAndFlush(existingAnswer);
        }
    }

    private AnswerResponse toResponse(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getDailyQuestion().getId(),
                answer.getContent(),
                answer.getUpdatedAt()
        );
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "답변은 1자 이상 2000자 이하로 입력해주세요.");
        }
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
