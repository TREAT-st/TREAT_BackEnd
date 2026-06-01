package com.example.demo.common.util;

import com.example.demo.domain.stock.exception.StockHandler;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExcelUtil {

    /** 엑셀 파일에서 특정 셀을 파싱합니다. **/
    public String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.format("%06d", (long) cell.getNumericCellValue());
            default -> "";
        };
    }

    /** 엑셀 파일이 유효한지 검증한다. **/
    public void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw StockHandler.invalidFile();
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw StockHandler.invalidFile();
        }
    }
}
