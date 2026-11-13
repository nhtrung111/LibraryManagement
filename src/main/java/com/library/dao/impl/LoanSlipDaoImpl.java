package com.library.dao.impl;

import org.springframework.stereotype.Repository;

import com.library.dao.LoanSlipDao;
import com.library.entity.LoanSlip;

@Repository
public class LoanSlipDaoImpl extends AbstractJpaDao<LoanSlip> implements LoanSlipDao {

    public LoanSlipDaoImpl() {
        super(LoanSlip.class);
    }
}
