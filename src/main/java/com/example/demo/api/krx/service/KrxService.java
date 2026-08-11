package com.example.demo.api.krx.service;

import com.example.demo.api.krx.client.KrxLambdaClient;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.exception.KrxHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrxService {

    private final KrxLambdaClient krxLambdaClient;

    public KrxOhlcvResponseDto getTopMarketCapOhlcv(int topN) {
        try {
            KrxOhlcvResponseDto response = krxLambdaClient.invokeGetTopMarketCapOhlcv(topN);

            if (response == null || response.getStocks() == null || response.getStocks().isEmpty()) {
                log.error("KRX Lambda 응답이 비어있습니다. topN={}", topN);
                throw KrxHandler.krxLambdaResponseEmpty();
            }

            return response;

        } catch (KrxHandler e) {
            throw e;
        } catch (Exception e) {
            log.error("KRX Lambda 호출 실패. topN={}", topN, e);
            throw KrxHandler.krxLambdaInvokeError();
        }
    }
}
