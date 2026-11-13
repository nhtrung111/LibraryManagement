package com.library.exception;

/** Every error the API can return, with its HTTP status (plan: Errors). */
public enum ErrorCode {
    VALIDATION_ERROR(400, "Request is invalid"),
    UNAUTHORIZED(401, "Login required"),
    INVALID_CREDENTIALS(401, "Wrong username or password"),
    TOKEN_EXPIRED(401, "Access token has expired"),
    INVALID_TOKEN(401, "Token is invalid"),
    REFRESH_TOKEN_REUSED(401, "Refresh token was already used; all sessions were logged out"),
    FORBIDDEN(403, "You do not have permission for this action"),
    NOT_FOUND(404, "Resource not found"),
    METHOD_NOT_ALLOWED(405, "HTTP method not supported for this path"),
    READER_NOT_FOUND(404, "Reader not found"),
    BOOK_NOT_FOUND(404, "Book not found"),
    READER_NOT_LINKED(404, "This account is not linked to a reader"),
    CONCURRENT_UPDATE(409, "Someone else changed this data at the same time; try again"),
    BOOK_NOT_BORROWED(422, "Reader has no unreturned copy of this book"),
    TOO_MANY_COPIES(422, "More copies scanned than the reader holds"),
    INTERNAL_ERROR(500, "Unexpected server error");

    private final int status;
    private final String defaultMessage;

    ErrorCode(int status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
