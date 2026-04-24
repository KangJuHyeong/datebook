package app.controller;

import app.dto.question.TodayQuestionResponse;
import app.service.DailyQuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final DailyQuestionService dailyQuestionService;

    public QuestionController(DailyQuestionService dailyQuestionService) {
        this.dailyQuestionService = dailyQuestionService;
    }

    @GetMapping("/today")
    public TodayQuestionResponse getTodayQuestion(HttpSession session) {
        return dailyQuestionService.getTodayQuestion(session);
    }
}
