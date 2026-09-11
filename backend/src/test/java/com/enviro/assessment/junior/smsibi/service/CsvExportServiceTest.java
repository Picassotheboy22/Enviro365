package com.enviro.assessment.junior.smsibi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.enviro.assessment.junior.smsibi.dto.WithdrawalResponse;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvExportServiceTest {

    private static final String HEADER =
            "Notice ID,Date,Investor,Product,Product Type,Amount,Balance Before,Balance After";

    private final CsvExportService service =
            new CsvExportService(Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void writesHeaderAndOneLinePerWithdrawal() {
        List<String> lines = export(
                row(1L, "Thabo Mokoena", "Retirement Annuity", ProductType.RETIREMENT),
                row(2L, "Lerato Dlamini", "Unit Trust Portfolio", ProductType.SAVINGS));

        assertThat(lines)
                .containsExactly(
                        HEADER,
                        "1,2026-09-01 14:30:00,Thabo Mokoena,Retirement Annuity,RETIREMENT,2500.00,10000.00,7500.00",
                        "2,2026-09-01 14:30:00,Lerato Dlamini,Unit Trust Portfolio,SAVINGS,2500.00,10000.00,7500.00");
    }

    @Test
    void writesOnlyTheHeaderWhenThereAreNoWithdrawals() {
        assertThat(export()).containsExactly(HEADER);
    }

    @Test
    void quotesValuesContainingCommasAndQuotes() {
        List<String> lines = export(row(1L, "Thabo Mokoena", "Growth Fund, \"Aggressive\"", ProductType.SAVINGS));

        assertThat(lines.get(1)).contains(",\"Growth Fund, \"\"Aggressive\"\"\",");
    }

    @Test
    void neutralisesValuesThatSpreadsheetsWouldRunAsFormulas() {
        List<String> lines = export(row(1L, "=1+1", "Savings", ProductType.SAVINGS));

        assertThat(lines.get(1)).contains(",'=1+1,");
    }

    @Test
    void suggestsADatedFileName() {
        assertThat(service.export(List.of()).filename()).isEqualTo("withdrawal-statement-2026-09-10.csv");
    }

    private List<String> export(WithdrawalResponse... rows) {
        byte[] content = service.export(List.of(rows)).content();
        return new String(content, StandardCharsets.UTF_8).lines().toList();
    }

    private static WithdrawalResponse row(Long id, String investorName, String productName, ProductType type) {
        return new WithdrawalResponse(
                id,
                7L,
                investorName,
                3L,
                productName,
                type,
                new BigDecimal("2500.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("7500.00"),
                LocalDateTime.of(2026, 9, 1, 14, 30));
    }
}
