package com.enviro.assessment.junior.smsibi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.exception.BusinessRuleException;
import com.enviro.assessment.junior.smsibi.exception.RuleViolation;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for the business rules. There is no Spring context or database, so they run in
 * milliseconds. The focus is on boundary values, because that is where off-by-one mistakes hide.
 */
class WithdrawalPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    private final WithdrawalPolicy policy = new WithdrawalPolicy();

    @Nested
    class RetirementAgeRule {

        @Test
        void rejectsRetirementWithdrawalWhenInvestorIsExactly65() {
            Product product = product(ProductType.RETIREMENT, "10000.00", investorBornOn(TODAY.minusYears(65)));

            assertViolation(product, "100.00", RuleViolation.RETIREMENT_AGE_RESTRICTION);
        }

        @Test
        void rejectsRetirementWithdrawalOnTheDayBefore66thBirthday() {
            Product product = product(
                    ProductType.RETIREMENT,
                    "10000.00",
                    investorBornOn(TODAY.minusYears(66).plusDays(1)));

            assertViolation(product, "100.00", RuleViolation.RETIREMENT_AGE_RESTRICTION);
        }

        @Test
        void allowsRetirementWithdrawalOn66thBirthday() {
            Product product = product(ProductType.RETIREMENT, "10000.00", investorBornOn(TODAY.minusYears(66)));

            assertThatCode(() -> policy.validate(product, new BigDecimal("100.00"), TODAY))
                    .doesNotThrowAnyException();
        }

        @Test
        void latestEligibleBirthDateMatchesTheRule() {
            LocalDate cutOff = WithdrawalPolicy.latestRetirementEligibleBirthDate(TODAY);

            // Born on the cut-off date: 66 today, so eligible. Born a day later: still 65, so not eligible.
            Product bornOnCutOff = product(ProductType.RETIREMENT, "100.00", investorBornOn(cutOff));
            Product bornDayAfter = product(ProductType.RETIREMENT, "100.00", investorBornOn(cutOff.plusDays(1)));

            assertThat(policy.restrictionFor(bornOnCutOff, TODAY)).isEmpty();
            assertThat(policy.restrictionFor(bornDayAfter, TODAY)).isPresent();
        }

        @Test
        void savingsProductsHaveNoAgeRestriction() {
            Product product = product(ProductType.SAVINGS, "10000.00", investorBornOn(TODAY.minusYears(30)));

            assertThatCode(() -> policy.validate(product, new BigDecimal("100.00"), TODAY))
                    .doesNotThrowAnyException();
            assertThat(policy.restrictionFor(product, TODAY)).isEmpty();
        }

        @Test
        void ageIsCheckedBeforeAmountSoTheUserSeesTheRealReason() {
            Product product = product(ProductType.RETIREMENT, "100.00", investorBornOn(TODAY.minusYears(40)));

            // Amount is also over the balance, but the age restriction is the more useful message.
            assertViolation(product, "5000.00", RuleViolation.RETIREMENT_AGE_RESTRICTION);
        }
    }

    @Nested
    class BalanceRules {

        @Test
        void allowsExactly90PercentOfBalance() {
            Product product = savings("1000.00");

            assertThatCode(() -> policy.validate(product, new BigDecimal("900.00"), TODAY))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsOneCentOver90Percent() {
            assertViolation(savings("1000.00"), "900.01", RuleViolation.EXCEEDS_WITHDRAWAL_LIMIT);
        }

        @Test
        void rejectsMoreThanTheBalanceWithItsOwnMessage() {
            assertThatThrownBy(() -> policy.validate(savings("1000.00"), new BigDecimal("1500.00"), TODAY))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getViolation()).isEqualTo(RuleViolation.INSUFFICIENT_BALANCE);
                        assertThat(ex.getMessage()).contains("R 1,500.00").contains("R 1,000.00");
                    });
        }

        @Test
        void rejectsZeroAndNegativeAmounts() {
            assertViolation(savings("1000.00"), "0.00", RuleViolation.INVALID_AMOUNT);
            assertViolation(savings("1000.00"), "-10.00", RuleViolation.INVALID_AMOUNT);
        }

        @Test
        void roundsTheLimitDownSoItCanNeverBeExceeded() {
            // 90% of 100.05 is 90.045. The limit must be 90.04, not 90.05.
            Product product = savings("100.05");

            assertThat(policy.maxWithdrawalAmount(product, TODAY)).isEqualByComparingTo("90.04");
            assertViolation(product, "90.05", RuleViolation.EXCEEDS_WITHDRAWAL_LIMIT);
        }
    }

    @Nested
    class MaxWithdrawalAmount {

        @Test
        void is90PercentOfBalanceForEligibleProducts() {
            assertThat(policy.maxWithdrawalAmount(savings("45500.00"), TODAY)).isEqualByComparingTo("40950.00");
        }

        @Test
        void isZeroForRestrictedProducts() {
            Product product = product(ProductType.RETIREMENT, "310000.00", investorBornOn(TODAY.minusYears(40)));

            assertThat(policy.maxWithdrawalAmount(product, TODAY)).isEqualByComparingTo("0.00");
            assertThat(policy.restrictionFor(product, TODAY))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("older than 65"));
        }
    }

    // ---- helpers ----

    private void assertViolation(Product product, String amount, RuleViolation expected) {
        assertThatThrownBy(() -> policy.validate(product, new BigDecimal(amount), TODAY))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> assertThat(ex.getViolation())
                        .isEqualTo(expected));
    }

    private static Investor investorBornOn(LocalDate dateOfBirth) {
        return new Investor("Test", "Investor", "test@example.com", null, dateOfBirth);
    }

    private static Product product(ProductType type, String balance, Investor investor) {
        return new Product(investor, "Test product", type, new BigDecimal(balance));
    }

    private static Product savings(String balance) {
        return product(ProductType.SAVINGS, balance, investorBornOn(TODAY.minusYears(30)));
    }
}
