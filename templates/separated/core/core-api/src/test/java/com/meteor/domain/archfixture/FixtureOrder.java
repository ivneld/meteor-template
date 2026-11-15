package com.meteor.domain.archfixture;

/** R-14 (다른 애그리거트를 필드로 가짐), R-15 (public setter), R-20 (public 생성자) 위반. */
public class FixtureOrder {

    private final FixtureMember member;

    private String status;

    public FixtureOrder(FixtureMember member) {
        this.member = member;
    }

    public void cancel() {
        status = "CANCELLED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public FixtureMember getMember() {
        return member;
    }

    public String getStatus() {
        return status;
    }

}
