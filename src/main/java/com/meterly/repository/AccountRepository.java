package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
