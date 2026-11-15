package com.meteor.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제 저장소. 애그리거트가 곧 엔티티이므로 Spring Data 인터페이스를 그대로 쓴다. 이 인터페이스를 부르는 것은
 * {@link PaymentReader} 와 {@link PaymentWriter} 뿐이다(R-04, R-09, R-10). 저장은 명시적으로
 * 한다(R-16).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
