package com.library.dao.impl;

import org.springframework.stereotype.Repository;

import com.library.dao.ReturnTransactionDao;
import com.library.entity.ReturnTransaction;

@Repository
public class ReturnTransactionDaoImpl extends AbstractJpaDao<ReturnTransaction>
        implements ReturnTransactionDao {

    public ReturnTransactionDaoImpl() {
        super(ReturnTransaction.class);
    }
}
