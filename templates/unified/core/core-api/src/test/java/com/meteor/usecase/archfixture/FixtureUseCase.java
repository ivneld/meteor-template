package com.meteor.usecase.archfixture;

import com.meteor.client.pg.PgApproval;
import com.meteor.client.pg.PgClient;

import org.springframework.transaction.annotation.Transactional;

/** R-18 위반: @Transactional 안에서 외부 시스템을 부른다. Spring 빈이 아니다. */
public class FixtureUseCase {

    private final PgClient pgClient;

    public FixtureUseCase(PgClient pgClient) {
        this.pgClient = pgClient;
    }

    @Transactional
    public PgApproval pay(String paymentKey, long amount) {
        return pgClient.approve(paymentKey, amount);
    }

}
