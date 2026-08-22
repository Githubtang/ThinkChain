package com.tyh.web.controller.chat;

import com.tyh.common.constant.HttpStatus;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ChatExceptionHandlerTest {

    @Test
    void rateLimitUsesRealHttp429AndMatchingJsonCode() {
        ResponseEntity<AjaxResult> response = new ChatExceptionHandler().handleServiceException(
                new ServiceException("访问过于频繁", HttpStatus.TOO_MANY_REQUESTS));

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).containsEntry(AjaxResult.CODE_TAG, 429);
    }
}
