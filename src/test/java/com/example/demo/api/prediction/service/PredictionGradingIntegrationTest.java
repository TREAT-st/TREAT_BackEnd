package com.example.demo.api.prediction.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.prediction.entity.*;
import com.example.demo.domain.prediction.port.KisPricePort;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import com.example.demo.domain.prediction.service.*;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.user.entity.*;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:prediction;NON_KEYWORDS=USER")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PredictionUseCase.class, PredictionGradingService.class,
        PredictionCommandServiceImpl.class, PredictionQueryServiceImpl.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PredictionGradingIntegrationTest {
    @Autowired PredictionUseCase useCase;
    @Autowired PredictionGradingService gradingService;
    @Autowired PredictionRepository repository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean KisPricePort pricePort;
    @MockitoBean StockQueryService stockQueryService;
    @MockitoBean UserPortfolioCommandService portfolioService;

    @Test
    void failedGradingRollsBackOnlyThatPredictionAndPersistsFailureSeparately() {
        List<Long> ids = createPredictions();
        when(pricePort.getCurrentPrice(anyString())).thenReturn(new BigDecimal("101"));
        doAnswer(invocation -> {
            // Flush the grade before failing, so the test verifies a real database rollback.
            entityManager.flush();
            throw new IllegalStateException("portfolio update failed");
        }).doNothing().doNothing().when(portfolioService).recordGradingResult(anyLong(), anyBoolean(), anyLong());

        var result = useCase.gradeMaturedPredictions();

        assertThat(result.getGradedCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isEqualTo(1);
        List<Prediction> predictions = repository.findAllById(ids);
        assertThat(predictions).filteredOn(p -> p.getStatus() == PredictionStatus.FAILED)
                .singleElement().satisfies(p -> assertThat(p.getGradedAt()).isNull());
        assertThat(predictions).filteredOn(p -> p.getStatus() == PredictionStatus.CORRECT)
                .hasSize(2).allSatisfy(p -> assertThat(p.getGradedAt()).isNotNull());
        // Failed rows are excluded from subsequent scheduled grading.
        assertThat(useCase.gradeMaturedPredictions().getGradedCount()).isZero();
    }

    @Test
    void priceFailureDoesNotPreventOtherPredictionsFromBeingGraded() {
        List<Long> ids = createPredictions();
        when(pricePort.getCurrentPrice(anyString()))
                .thenReturn(new BigDecimal("101"))
                .thenThrow(new IllegalStateException("price unavailable"))
                .thenReturn(new BigDecimal("101"));

        var result = useCase.gradeMaturedPredictions();

        assertThat(result.getGradedCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(repository.findAllById(ids)).extracting(Prediction::getStatus)
                .containsExactlyInAnyOrder(PredictionStatus.CORRECT, PredictionStatus.FAILED, PredictionStatus.CORRECT);
        verify(portfolioService, times(2)).recordGradingResult(anyLong(), eq(true), eq(10L));
    }

    @Test
    void failureRecordingDoesNotOverwriteCompletedGrading() {
        List<Long> ids = createPredictions();
        when(pricePort.getCurrentPrice(anyString())).thenReturn(new BigDecimal("101"));
        useCase.gradeMaturedPredictions();

        gradingService.markFailed(ids.get(0));

        assertThat(repository.findById(ids.get(0)).orElseThrow().getStatus()).isEqualTo(PredictionStatus.CORRECT);
    }

    private List<Long> createPredictions() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            repository.deleteAll();
            User user = User.builder().username(UUID.randomUUID().toString())
                    .role(Role.values()[0]).status(UserStatus.ACTIVE).build();
            entityManager.persist(user);
            Stock stock = Stock.builder().stockCode(UUID.randomUUID().toString().substring(0, 20))
                    .stockName("test").build();
            entityManager.persist(stock);
            return java.util.stream.IntStream.range(0, 3).mapToObj(i -> repository.save(Prediction.builder()
                    .user(user).stock(stock).duration(PredictionDuration.ONE_DAY).target(PredictionTarget.UNDER_3)
                    .basePrice(new BigDecimal("100")).maturityAt(LocalDateTime.now().minusDays(1))
                    .earnablePoints(10L).build()).getId()).toList();
        });
    }
}
