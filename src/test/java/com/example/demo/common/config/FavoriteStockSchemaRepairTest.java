package com.example.demo.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FavoriteStockSchemaRepairTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final FavoriteStockSchemaRepair repair = new FavoriteStockSchemaRepair(jdbcTemplate);

    @Test
    void autoIncrement가_없으면_기존_PK_컬럼만_보정한다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("");

        repair.repairAutoIncrement();

        verify(jdbcTemplate).execute(contains("AUTO_INCREMENT"));
    }

    @Test
    void autoIncrement가_있으면_스키마를_변경하지_않는다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("auto_increment");

        repair.repairAutoIncrement();

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
