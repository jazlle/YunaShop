package com.yuna.yunashop.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, String> {
}
