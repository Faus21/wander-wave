package com.dama.wanderwave.handler;

import com.dama.wanderwave.handler.azure.FileTypeException;
import com.dama.wanderwave.handler.chat.ChatRoomException;
import com.dama.wanderwave.handler.chat.ChatRoomNotFoundException;
import com.dama.wanderwave.handler.comment.CommentNotFoundException;
import com.dama.wanderwave.handler.email.EmailSendingException;
import com.dama.wanderwave.handler.email.EmailTemplateException;
import com.dama.wanderwave.handler.post.CategoryTypeNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.report.*;
import com.dama.wanderwave.handler.role.RoleNotFoundException;
import com.dama.wanderwave.handler.token.TokenExpiredException;
import com.dama.wanderwave.handler.token.TokenInvalidException;
import com.dama.wanderwave.handler.token.TokenNotFoundException;
import com.dama.wanderwave.handler.token.TokenRefreshException;
import com.dama.wanderwave.handler.user.BannedUserException;
import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UniqueConstraintViolationException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.handler.user.like.IsLikedException;
import com.dama.wanderwave.handler.user.like.LikeNotFoundException;
import com.dama.wanderwave.handler.user.save.IsSavedException;
import com.dama.wanderwave.handler.user.save.SavedPostNotFound;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.ApplicationContextException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.IdentityHashMap;
import java.util.Map;


import static org.springframework.http.HttpStatus.*;

public enum ExceptionStatus {
    // NOT_FOUND
    USER_NOT_FOUND(UserNotFoundException.class, NOT_FOUND),
    ROLE_NOT_FOUND(RoleNotFoundException.class, NOT_FOUND),
    TOKEN_NOT_FOUND(TokenNotFoundException.class, NOT_FOUND),
    CHAT_ROOM_NOT_FOUND(ChatRoomNotFoundException.class, NOT_FOUND),
    POST_NOT_FOUND(PostNotFoundException.class, NOT_FOUND),
    COMMENT_NOT_FOUND(CommentNotFoundException.class, NOT_FOUND),
    REPORT_NOT_FOUND(ReportNotFoundException.class, NOT_FOUND),
    REPORT_TYPE_NOT_FOUND(ReportTypeNotFoundException.class, NOT_FOUND),
    REPORT_STATUS_NOT_FOUND(ReportStatusNotFoundException.class, NOT_FOUND),
    CATEGORY_TYPE_NOT_FOUND(CategoryTypeNotFoundException.class, NOT_FOUND),
    LIKE_NOT_FOUND(LikeNotFoundException.class, NOT_FOUND),
    SAVED_POST_NOT_FOUND(SavedPostNotFound.class, NOT_FOUND),

    // BAD_REQUEST
    METHOD_ARGUMENT_NOT_VALID(MethodArgumentNotValidException.class, BAD_REQUEST),
    CONSTRAINT_VIOLATION(ConstraintViolationException.class, BAD_REQUEST),
    TOKEN_INVALID(TokenInvalidException.class, BAD_REQUEST),
    TOKEN_REFRESH(TokenRefreshException.class, BAD_REQUEST),
    TOKEN_EXPIRED(TokenExpiredException.class, BAD_REQUEST),
    WRONG_REPORT_OBJECT(WrongReportObjectException.class, BAD_REQUEST),
    DUPLICATE_REPORT(DuplicateReportException.class, BAD_REQUEST),
    BANNED_USER(BannedUserException.class, BAD_REQUEST),
    IS_LIKED(IsLikedException.class, BAD_REQUEST),
    IS_SAVED(IsSavedException.class, BAD_REQUEST),

    FILE_TYPE_EXCEPTION(FileTypeException.class, BAD_REQUEST),
    // UNAUTHORIZED
    BAD_CREDENTIALS(BadCredentialsException.class, UNAUTHORIZED),
    UNAUTHORIZED_ACTION(UnauthorizedActionException.class, FORBIDDEN),
    // CONFLICT
    UNIQUE_CONSTRAINT_VIOLATION(UniqueConstraintViolationException.class, CONFLICT),

    // INTERNAL_SERVER_ERROR
    EMAIL_TEMPLATE(EmailTemplateException.class, INTERNAL_SERVER_ERROR),
    EMAIL_SENDING(EmailSendingException.class, INTERNAL_SERVER_ERROR),
    GENERIC_EXCEPTION(Exception.class, INTERNAL_SERVER_ERROR),
    CHAT_ROOM_EXCEPTION(ChatRoomException.class, INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(InternalError.class, INTERNAL_SERVER_ERROR),
    APPLICATION_CONTEXT_EXCEPTION(ApplicationContextException.class, INTERNAL_SERVER_ERROR);

    private static final Map<Class<? extends Throwable>, HttpStatus> EXCEPTION_STATUS_MAP;

    static {
        EXCEPTION_STATUS_MAP = new IdentityHashMap<>(values().length);
        for (ExceptionStatus status : values()) {
            EXCEPTION_STATUS_MAP.put(status.exceptionClass, status.status);
        }
    }

    private final Class<? extends Throwable> exceptionClass;
    private final HttpStatus status;

    ExceptionStatus(Class<? extends Throwable> exceptionClass, HttpStatus status) {
        this.exceptionClass = exceptionClass;
        this.status = status;
    }

    public static HttpStatus getStatusFor(Throwable throwable) {
        Class<?> exceptionClass = throwable.getClass();
        HttpStatus status;
        do {
            status = EXCEPTION_STATUS_MAP.get(exceptionClass);
            if (status != null) {
                return status;
            }
            exceptionClass = exceptionClass.getSuperclass();
        } while (exceptionClass != null);
        return INTERNAL_SERVER_ERROR;
    }
}