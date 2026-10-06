package com.meteor.member.application;

import com.meteor.member.domain.Email;

public record MemberRegisterCommand(Email email, String name) {
}
