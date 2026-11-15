package com.meteor.domain.archfixture;

import jakarta.persistence.ManyToOne;

/** R-14 위반: JPA 연관관계. @Entity 를 붙이지 않아 엔티티 스캔에 걸리지 않는다. */
public class FixtureAssociation {

    @ManyToOne
    private FixtureMember member;

    public FixtureMember getMember() {
        return member;
    }

}
