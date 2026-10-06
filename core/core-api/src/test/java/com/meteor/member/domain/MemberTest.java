package com.meteor.member.domain;

import com.meteor.support.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void registeredMemberIsActive() {
        Member member = Member.register(Email.of("a@b.com"), "kim");

        assertThat(member.isNew()).isTrue();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void withdrawTwiceIsRejected() {
        Member member = Member.restore(1L, Email.of("a@b.com"), "kim", MemberStatus.ACTIVE);
        member.withdraw();

        assertThat(member.isActive()).isFalse();
        assertThatThrownBy(member::withdraw).isInstanceOf(CoreException.class);
    }

}
