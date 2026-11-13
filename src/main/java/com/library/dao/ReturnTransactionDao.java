package com.library.dao;

import java.util.Optional;

import com.library.entity.ReturnTransaction;

public interface ReturnTransactionDao {

    Optional<ReturnTransaction> findById(Long id);

    ReturnTransaction save(ReturnTransaction transaction);
}
