package com.library.dao.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.library.dao.ReaderDao;
import com.library.entity.Reader;

@Repository
public class ReaderDaoImpl extends AbstractJpaDao<Reader> implements ReaderDao {

    public ReaderDaoImpl() {
        super(Reader.class);
    }

    @Override
    public Optional<Reader> findByCardBarcode(String cardBarcode) {
        return first(em.createQuery(
                        "select r from Reader r where r.cardBarcode = :barcode", Reader.class)
                .setParameter("barcode", cardBarcode));
    }
}
