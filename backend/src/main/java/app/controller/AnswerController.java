package app.controller;

import app.dto.answer.AnswerResponse;
import app.dto.answer.CreateAnswerRequest;
import app.dto.answer.UpdateAnswerRequest;
import app.service.AnswerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping
    public AnswerResponse createAnswer(@Valid @RequestBody CreateAnswerRequest request, HttpSession session) {
        return answerService.createOrUpdateAnswer(request, session);
    }

    @PutMapping("/{answerId}")
    public AnswerResponse updateAnswer(
            @PathVariable("answerId") Long answerId,
            @Valid @RequestBody UpdateAnswerRequest request,
            HttpSession session
    ) {
        return answerService.updateAnswer(answerId, request, session);
    }
}
