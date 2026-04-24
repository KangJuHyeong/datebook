package app.controller;

import app.dto.couple.CreateCoupleResponse;
import app.dto.couple.JoinCoupleRequest;
import app.dto.couple.JoinCoupleResponse;
import app.service.CoupleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/couples")
public class CoupleController {

    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    @PostMapping
    public CreateCoupleResponse createCouple(HttpSession session) {
        return coupleService.createCouple(session);
    }

    @PostMapping("/join")
    public JoinCoupleResponse joinCouple(@Valid @RequestBody JoinCoupleRequest request, HttpSession session) {
        return coupleService.joinCouple(request, session);
    }
}
