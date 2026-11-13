package com.library.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.library.dao.LoanItemDao;
import com.library.entity.LoanItem;
import com.library.entity.LoanStatus;

@Repository
public class LoanItemDaoImpl extends AbstractJpaDao<LoanItem> implements LoanItemDao {

    public LoanItemDaoImpl() {
        super(LoanItem.class);
    }

    @Override
    public List<LoanItem> findBorrowedByReaderOrdered(Long readerId) {
        return em.createQuery("""
                        select li from LoanItem li
                        join fetch li.loanSlip s
                        join fetch s.reader r
                        join fetch li.book b
                        where r.id = :readerId and li.status = :status
                        order by s.borrowDate, s.id, li.id
                        """, LoanItem.class)
                .setParameter("readerId", readerId)
                .setParameter("status", LoanStatus.BORROWED)
                .getResultList();
    }

    @Override
    public List<LoanItem> findReturnedByReader(Long readerId) {
        return em.createQuery("""
                        select li from LoanItem li
                        join fetch li.loanSlip s
                        join fetch s.reader r
                        join fetch li.book b
                        where r.id = :readerId and li.status = :status
                        order by li.returnDate desc, li.id
                        """, LoanItem.class)
                .setParameter("readerId", readerId)
                .setParameter("status", LoanStatus.RETURNED)
                .getResultList();
    }
}
