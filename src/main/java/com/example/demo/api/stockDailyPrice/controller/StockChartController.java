package com.example.demo.api.stockDailyPrice.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.StockChartResponse;
import com.example.demo.api.stockDailyPrice.service.StockChartUseCase;
import com.example.demo.domain.stockDailyPrice.entity.ChartRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.example.demo.common.consts.StaticVariable.CHART_RANGE_MESSAGE;
import static com.example.demo.common.consts.StaticVariable.CHART_RANGE_REGEXP;

/**
 * 종목 차트. 외부에서 보는 리소스는 "종목의 차트"라 URL은 /stocks 아래에 두고,
 * 구현은 일봉 데이터라 코드는 일봉 도메인이 갖는다. Swagger 태그도 종목 API와 같게 둔다.
 *
 * @Validated가 있어야 @PathVariable·@RequestParam의 @Pattern이 400으로 처리된다.
 */
@Tag(name = "[주식] 주식 종목 API.")
@Validated
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockChartController {

    private final StockChartUseCase stockChartUseCase;

    @Operation(summary = "종목 차트 조회",
            description = "종목의 일봉으로 차트 데이터를 만듭니다. KRX를 호출하지 않고 서버에 쌓인 데이터만 읽습니다.<br>" +
                    "모든 range가 일봉이며 interval은 항상 day입니다.<br>" +
                    "기간은 [끝 − range, 끝]이고 양 끝을 포함합니다. 예) 끝이 2026-10-21이면 1Y는 2025-10-21 ~ 2026-10-21.<br>" +
                    "끝은 그 종목에 쌓인 마지막 거래일입니다. 시세는 하루 한 번 직전 거래일까지 반영되며 실시간이 아닙니다.<br>" +
                    "휴장일·주말은 일봉이 없어 빠집니다. 개수는 대략 1W 6, 1M 22, 3M 64, 1Y 250, 3Y 740개입니다.<br>" +
                    "아직 일봉이 쌓이지 않은 종목은 data가 빈 배열입니다. 없는 종목은 404(4250)입니다.<br>" +
                    "고가·저가를 받지 못한 날은 highPrice·lowPrice가 null입니다.")
    @GetMapping("/{stockCode}/chart")
    public ApiResponseDto<StockChartResponse> getStockChart(
            @PathVariable @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자입니다.") String stockCode,
            @RequestParam(defaultValue = "3M")
            @Pattern(regexp = CHART_RANGE_REGEXP, message = CHART_RANGE_MESSAGE) String range) {
        // @Pattern을 통과했으므로 항상 찾는다.
        ChartRange chartRange = ChartRange.fromCode(range).orElseThrow();
        return ApiResponseDto.onSuccess(stockChartUseCase.getStockChart(stockCode, chartRange));
    }
}
