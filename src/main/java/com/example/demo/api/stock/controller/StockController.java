package com.example.demo.api.stock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stock.dto.StockResponseDto.*;
import com.example.demo.api.stock.service.StockUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.stock.entity.StockSortType;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[주식] 주식 종목 API.")
@Validated
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;

    @Operation(summary = "코스피200 종목·시세 동기화 (KRX)",
            description = "KRX Lambda로 코스피200 구성종목과 가장 가까운 거래일의 시가/종가를 한 번에 받아 반영합니다.<br>" +
                    "편출된 종목은 삭제하지 않고 비활성 처리하며, 재편입되면 다시 활성으로 되돌립니다.<br><br>" +
                    "아래는 에러가 발생한 종목들입니다.<br>" +
                    "priceUnavailableStockCodes : 목록에는 반영됐지만 시세를 못 받은 종목(거래정지 등)<br>" +
                    "unresolvedStockCodes : 종목명을 못 받아 목록에 반영하지 못한 종목(활성 상태 유지)<br>" +
                    "priceUpdateSkippedStockCodes : 시세는 받았지만 DB에 반영하지 못한 종목. " +
                    "이 목록이 비어 있지 않으면 동기화 정합성 이상 신호입니다.<br><br>" +
                    "<b>거래일 검사</b><br>" +
                    "Lambda는 항상 오늘을 제외한 직전 거래일을 반환하므로 실행일과 거래일은 평일에도 다릅니다. " +
                    "새 데이터 여부는 거래일과 DB 최신 거래일의 비교로 판단합니다.<br>" +
                    "· 거래일 &gt; DB 최신 : 실행<br>" +
                    "· 거래일 = DB 최신 : 409(4252). 주말·휴장일에는 직전 거래일이 반복해서 내려오므로 정상입니다<br>" +
                    "· 거래일 &lt; DB 최신 : 409(4253). force로도 허용하지 않습니다<br>" +
                    "· 거래일 ≥ 오늘 : 502(4254). Lambda 응답이 깨진 경우입니다<br><br>" +
                    "force=true : 이미 반영된 <b>같은 거래일</b>만 재실행합니다. " +
                    "priceUnavailableStockCodes에 남은 종목의 시세를 다시 받으려 할 때 사용하세요. " +
                    "과거 거래일과 오늘 이후 날짜는 force와 무관하게 막힙니다.")
    @PostMapping("/sync")
    public ApiResponseDto<SyncStocksResponse> syncKospi200FromKrx(
            @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponseDto.onSuccess(stockUseCase.syncKospi200FromKrx(force));
    }

    @Operation(summary = "종목 코드로 해당 종목 조회",
            description = "종목 코드로 코스피200 종목의 시가/종가를 조회합니다.<br>" +
                    "tradeDate는 해당 시세의 기준 거래일(당일과 가장 가까운 거래일)이며, 동기화 전이면 시세가 null일 수 있습니다.<br>" +
                    "편출된 종목도 조회되며 isActive로 현재 편입 여부를 알 수 있습니다.")
    @GetMapping("/{stockCode}")
    public ApiResponseDto<StockOpenAndClosePriceResponse> getStockByStockcode(
            @PathVariable @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자입니다.") String stockCode) {
        return ApiResponseDto.onSuccess(stockUseCase.getStockOpenAndClosePrice(stockCode));
    }

    @Operation(summary = "모든 종목 조회",
            description = "Stock에 저장된 종목을 페이지 단위로 조회합니다.<br>" +
                    "Stock은 종목별 최신 스냅샷만 보관하므로 항상 마지막으로 동기화된 시세가 나옵니다. " +
                    "각 종목의 기준 거래일은 응답의 tradeDate로 확인하세요. " +
                    "시세를 못 받은 종목은 이전 거래일 값이 남아 있어 종목마다 tradeDate가 다를 수 있습니다.<br><br>" +
                    "isActive=true : 코스피200에 현재 편입된 종목만<br>" +
                    "isActive=false 또는 생략 : 편입, 편출 전체 종목<br><br>" +
                    "sortBy=STOCK_CODE : 종목코드 오름차순(기본값)<br>" +
                    "sortBy=MARKET_CAP : 시가총액 내림차순<br>" +
                    "sortBy=LIKE_COUNT : 관심등록수 내림차순<br>" +
                    "pageSize는 1~200입니다.")
    @GetMapping("/all-stock")
    public ApiResponseDto<StockPageResponse> getAllStocks(
            @AuthUser User user,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "STOCK_CODE") StockSortType sortBy,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
            @Max(value = 200, message = "pageSize는 200 이하여야 합니다.") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, sortBy.getSort());
        return ApiResponseDto.onSuccess(stockUseCase.getAllStocks(user.getId(), isActive, pageable));
    }
}
