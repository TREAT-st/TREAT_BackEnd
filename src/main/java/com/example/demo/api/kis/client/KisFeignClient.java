package com.example.demo.api.kis.client;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.dto.KisTokenRequestDto;
import com.example.demo.api.kis.dto.KisTokenResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kisClient", url = "${kis.base-url}")
public interface KisFeignClient {

    @PostMapping("/oauth2/tokenP")
    KisTokenResponseDto issueToken(@RequestBody KisTokenRequestDto request);

    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
    KisResponseDto getDailyStockPrice(
            @RequestHeader("authorization") String authorization,
            @RequestHeader("appkey")        String appKey,
            @RequestHeader("appsecret")     String appSecret,
            @RequestHeader("tr_id")         String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String fidCondMrktDivCode,
            @RequestParam("FID_INPUT_ISCD")          String fidInputIscd,
            @RequestParam("FID_INPUT_DATE_1")        String fidInputDate1,
            @RequestParam("FID_INPUT_DATE_2")        String fidInputDate2,
            @RequestParam("FID_PERIOD_DIV_CODE")     String fidPeriodDivCode,
            @RequestParam("FID_ORG_ADJ_PRC")         String fidOrgAdjPrc
    );
}