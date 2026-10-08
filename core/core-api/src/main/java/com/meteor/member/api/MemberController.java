package com.meteor.member.api;

import com.meteor.member.api.request.MemberRegisterRequest;
import com.meteor.member.api.response.MemberRegisteredResponse;
import com.meteor.member.api.response.MemberResponse;
import com.meteor.member.application.MemberUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberUseCase memberUseCase;

    public MemberController(MemberUseCase memberUseCase) {
        this.memberUseCase = memberUseCase;
    }

    @PostMapping
    public MemberRegisteredResponse register(@RequestBody @Valid MemberRegisterRequest request) {
        return new MemberRegisteredResponse(memberUseCase.register(request.toCommand()));
    }

    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable Long memberId) {
        return MemberResponse.from(memberUseCase.find(memberId));
    }

    @PostMapping("/{memberId}/withdraw")
    public MemberResponse withdraw(@PathVariable Long memberId) {
        return MemberResponse.from(memberUseCase.withdraw(memberId));
    }

}
