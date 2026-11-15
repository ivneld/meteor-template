package com.meteor.domain.archfixture;

/** R-16 (바꾸고 저장하지 않음), R-20 (다른 애그리거트의 생성자 호출), R-21 (restore 로 조립) 위반. */
public class FixtureOrderWriter {

    private final FixtureOrderRepository fixtureOrderRepository;

    public FixtureOrderWriter(FixtureOrderRepository fixtureOrderRepository) {
        this.fixtureOrderRepository = fixtureOrderRepository;
    }

    public FixtureOrder cancel(Long orderId) {
        FixtureOrder order = fixtureOrderRepository.findById(orderId).orElseThrow();
        order.cancel();
        return order;
    }

    public FixtureOrder place(Long memberId) {
        return fixtureOrderRepository.save(new FixtureOrder(FixtureMember.restore(memberId)));
    }

}
