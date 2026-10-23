package com.example.demo.security.batch;

import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.batch.exception.BatchErrorStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** /api/v1/batch/**는 permitAll이라 이 검증이 유일한 인증이다. */
class BatchSecretVerifierTest {

    private final BatchSecretVerifier verifier = new BatchSecretVerifier("test-batch-secret");

    @Test
    void 시크릿이_없으면_401_코드로_거부한다() {
        assertThatThrownBy(() -> verifier.verify(null))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED.getCode());
    }

    @Test
    void 시크릿이_틀리면_거부한다() {
        assertThatThrownBy(() -> verifier.verify("wrong")).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> verifier.verify("")).isInstanceOf(GeneralException.class);
        // 앞부분만 같은 값도 거부해야 한다
        assertThatThrownBy(() -> verifier.verify("test-batch-secret-extra")).isInstanceOf(GeneralException.class);
    }

    @Test
    void 시크릿이_맞으면_통과한다() {
        assertThatCode(() -> verifier.verify("test-batch-secret")).doesNotThrowAnyException();
    }
}
