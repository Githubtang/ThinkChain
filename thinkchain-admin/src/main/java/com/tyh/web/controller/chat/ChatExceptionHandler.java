package com.tyh.web.controller.chat;

import com.tyh.common.constant.HttpStatus;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.exception.ServiceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * AI 接口专用异常响应转换器。
 *
 * <p>系统旧接口把业务码写在 JSON 中；AI 接口额外把合法的 4xx/5xx 业务码写入真实 HTTP 状态，
 * 因此前端能够直接识别 401、403、429 等结果。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.tyh.web.controller.chat")
public class ChatExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<AjaxResult> handleServiceException(ServiceException exception) {
        int code = exception.getCode() != null ? exception.getCode() : HttpStatus.ERROR;
        int httpStatus = code >= 400 && code <= 599 ? code : HttpStatus.ERROR;
        return ResponseEntity.status(httpStatus).body(AjaxResult.error(code, exception.getMessage()));
    }
}
