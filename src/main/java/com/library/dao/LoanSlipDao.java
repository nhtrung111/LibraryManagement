package com.library.dao;

import java.util.Optional;

import com.library.entity.LoanSlip;

public interface LoanSlipDao {

    Optional<LoanSlip> findById(Long id);

    LoanSlip save(LoanSlip slip);
}
