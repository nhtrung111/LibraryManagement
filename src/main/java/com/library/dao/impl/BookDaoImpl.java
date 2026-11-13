package com.library.dao.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.library.dao.BookDao;
import com.library.entity.Book;

@Repository
public class BookDaoImpl extends AbstractJpaDao<Book> implements BookDao {

    public BookDaoImpl() {
        super(Book.class);
    }

    @Override
    public Optional<Book> findByBarcode(String barcode) {
        return first(em.createQuery(
                        "select b from Book b where b.barcode = :barcode", Book.class)
                .setParameter("barcode", barcode));
    }

    @Override
    public List<Book> search(String text) {
        if (text == null || text.isBlank()) {
            return em.createQuery("select b from Book b order by b.title", Book.class)
                    .getResultList();
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return em.createQuery("""
                        select b from Book b
                        where lower(b.title) like :p
                           or lower(b.author) like :p
                           or lower(b.code) like :p
                        order by b.title
                        """, Book.class)
                .setParameter("p", pattern)
                .getResultList();
    }
}
