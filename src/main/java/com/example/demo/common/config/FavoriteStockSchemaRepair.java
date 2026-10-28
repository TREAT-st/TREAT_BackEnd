package com.example.demo.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 기존 운영 DB에서 favorite_stock PK의 AUTO_INCREMENT가 빠진 경우를 보정한다.
 *
 * Hibernate의 ddl-auto=update는 기존 컬럼의 IDENTITY 속성을 복구하지 않을 수 있다.
 * 이 보정은 현재 DB의 메타데이터를 확인한 뒤 필요한 경우에만 실행되며,
 * 기존 행과 PK 값은 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
public class FavoriteStockSchemaRepair {
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void repairAutoIncrement() {
        String extra = jdbcTemplate.queryForObject("""
                SELECT EXTRA
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'favorite_stock'
                  AND COLUMN_NAME = 'favorite_stock_id'
                """, String.class);

        if (extra != null && extra.toLowerCase(Locale.ROOT).contains("auto_increment")) {
            return;
        }

        jdbcTemplate.execute("""
                ALTER TABLE favorite_stock
                MODIFY COLUMN favorite_stock_id BIGINT NOT NULL AUTO_INCREMENT
                """);
        log.info("favorite_stock.favorite_stock_id AUTO_INCREMENT 속성을 복구했습니다.");
    }
}
