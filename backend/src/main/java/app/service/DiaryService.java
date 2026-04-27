package app.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.domain.Answer;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.PartnerAnswerStatus;
import app.dto.diary.DiaryAnswerResponse;
import app.dto.diary.DiaryEntryResponse;
import app.dto.diary.DiaryResponse;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final AnswerRepository answerRepository;

    public DiaryService(
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

    public DiaryResponse getDiary(HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        Long coupleId = coupleMember.getCouple().getId();
        Long currentUserId = coupleMember.getUser().getId();

        List<DailyQuestion> dailyQuestions = dailyQuestionRepository.findAllByCouple_IdOrderByQuestionDateDesc(coupleId);
        if (dailyQuestions.isEmpty()) {
            return new DiaryResponse(List.of());
        }

        Map<Long, List<Answer>> answersByDailyQuestionId = answerRepository.findAllByDailyQuestion_IdIn(
                        dailyQuestions.stream().map(DailyQuestion::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(answer -> answer.getDailyQuestion().getId()));

        List<DiaryEntryResponse> entries = dailyQuestions.stream()
                .map(dailyQuestion -> toEntry(dailyQuestion, currentUserId, answersByDailyQuestionId))
                .toList();

        return new DiaryResponse(entries);
    }

    private DiaryEntryResponse toEntry(
            DailyQuestion dailyQuestion,
            Long currentUserId,
            Map<Long, List<Answer>> answersByDailyQuestionId
    ) {
        List<Answer> answers = answersByDailyQuestionId.getOrDefault(dailyQuestion.getId(), List.of());
        Map<Boolean, List<Answer>> answersByOwnership = answers.stream()
                .collect(Collectors.partitioningBy(answer -> answer.getUser().getId().equals(currentUserId)));
        Answer myAnswer = answersByOwnership.getOrDefault(true, List.of()).stream().findFirst().orElse(null);
        Answer partnerAnswer = answersByOwnership.getOrDefault(false, List.of()).stream().findFirst().orElse(null);
        PartnerAnswerStatus partnerAnswerStatus = resolvePartnerAnswerStatus(myAnswer, partnerAnswer);

        return new DiaryEntryResponse(
                dailyQuestion.getId(),
                dailyQuestion.getQuestionDate(),
                dailyQuestion.getQuestion().getContent(),
                myAnswer == null ? "NOT_ANSWERED" : "ANSWERED",
                partnerAnswerStatus.name(),
                myAnswer == null ? null : new DiaryAnswerResponse(myAnswer.getUser().getDisplayName(), myAnswer.getContent()),
                partnerAnswerStatus == PartnerAnswerStatus.REVEALED
                        ? new DiaryAnswerResponse(partnerAnswer.getUser().getDisplayName(), partnerAnswer.getContent())
                        : null,
                myAnswer != null && partnerAnswer != null
        );
    }

    private PartnerAnswerStatus resolvePartnerAnswerStatus(Answer myAnswer, Answer partnerAnswer) {
        if (myAnswer != null && partnerAnswer != null) {
            return PartnerAnswerStatus.REVEALED;
        }
        if (partnerAnswer != null) {
            return PartnerAnswerStatus.ANSWERED_LOCKED;
        }
        return PartnerAnswerStatus.LOCKED;
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
