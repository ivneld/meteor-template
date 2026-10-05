package com.meteor.member.application;

import com.meteor.shared.Email;

public record MemberRegisterCommand(Email email, String name) {
}
