package com.meteor.domain.archfixture;

/** R-17 위반: 조회 서비스가 저장한다. */
public class FixtureOrderReader {

    private final FixtureOrderRepository fixtureOrderRepository;

    public FixtureOrderReader(FixtureOrderRepository fixtureOrderRepository) {
        this.fixtureOrderRepository = fixtureOrderRepository;
    }

    public FixtureOrder find(Long orderId) {
        FixtureOrder order = fixtureOrderRepository.findById(orderId).orElseThrow();
        return fixtureOrderRepository.save(order);
    }

}
