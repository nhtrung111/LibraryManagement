package com.library.dto;

import com.library.entity.Reader;

public record ReaderInfo(String code, String name, String cardBarcode) {

    public static ReaderInfo from(Reader reader) {
        return new ReaderInfo(reader.getCode(), reader.getName(), reader.getCardBarcode());
    }
}
