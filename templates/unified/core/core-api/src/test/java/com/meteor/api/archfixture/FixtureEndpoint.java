package com.meteor.api.archfixture;

import com.meteor.domain.Money;

/** R-03 (이름이 *Controller 가 아님), R-19 (값 객체 연산) 위반. Spring 빈이 아니다. */
public class FixtureEndpoint {

    public long total(long unitPrice, int quantity) {
        return Money.of(unitPrice).times(quantity).amount();
    }

}
