package com.meteor.domain.archfixture;

/** 애그리거트 fixture. */
public class FixtureMember {

    private final Long id;

    private FixtureMember(Long id) {
        this.id = id;
    }

    public static FixtureMember register(Long id) {
        return new FixtureMember(id);
    }

    public Long getId() {
        return id;
    }

}
