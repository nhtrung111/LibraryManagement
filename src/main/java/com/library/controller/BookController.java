package com.library.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.BookResponse;
import com.library.service.BookService;

import lombok.RequiredArgsConstructor;

/** USER or STAFF (US-6). */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookResponse> search(@RequestParam(name = "q", required = false) String q) {
        return bookService.search(q);
    }

    @GetMapping("/{barcode}/availability")
    public BookResponse availability(@PathVariable String barcode) {
        return bookService.availability(barcode);
    }
}
