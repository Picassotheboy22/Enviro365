package com.enviro.assessment.junior.smsibi.service;

import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.exception.BusinessRuleException;
import com.enviro.assessment.junior.smsibi.exception.RuleViolation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The single home for Enviro365's withdrawal business rules.
 *
 * <p>Keeping all the rules in one class, instead of scattering them through services and controllers, means:
 * <ul>
 *   <li>they can be unit-tested in isolation, including edge cases (age exactly 65, exactly 90%);</li>
 *   <li>the portfolio endpoint reuses them to tell the UI up-front what is allowed;</li>
 *   <li>a rule change happens in exactly one place.</li>
 * </ul>
 * The class is stateless, so it is safe to share as a Spring singleton.
 *
 * <p>The amount rules use the product's <em>available</em> balance: the balance minus money already on hold for open
 * (pending or approved) notices. That gives the same answer as if every open notice had already been paid. With a
 * balance of R 100,000 and R 90,000 on hold, R 10,000 is available and at most R 9,000 can be requested, which is
 * exactly what the 90% rule allows once the first notice is paid. Without this, several open notices could together
 * promise more money than the product holds.
 */
@Component
public class WithdrawalPolicy {

    /**
     * The brief says "retirement withdrawals only allowed if age &gt; 65". This is read as strictly greater,
     * so an investor who is exactly 65 is NOT yet eligible.
     */
    public static final int RETIREMENT_AGE_THRESHOLD = 65;

    /** The brief says "withdrawal must not exceed 90% of balance". */
    public static final BigDecimal MAX_WITHDRAWAL_RATIO = new BigDecimal("0.90");

    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

    /**
     * The latest date of birth that is old enough, today, for retirement withdrawals: anyone born on or before it is at
     * least 66, i.e. "older than 65". Lets the dashboard count eligible investors with a single database query.
     */
    public static LocalDate latestRetirementEligibleBirthDate(LocalDate today) {
        return today.minusYears(RETIREMENT_AGE_THRESHOLD + 1L);
    }

    /**
     * Why this product cannot be withdrawn from at all, regardless of amount, or empty if it can.
     */
    public Optional<String> restrictionFor(Product product, LocalDate today) {
        if (product.getType() == ProductType.RETIREMENT) {
            int age = product.getInvestor().ageOn(today);
            if (age <= RETIREMENT_AGE_THRESHOLD) {
                return Optional.of("Retirement withdrawals are only allowed for investors older than "
                        + RETIREMENT_AGE_THRESHOLD + ". The investor is " + age + ".");
            }
        }
        return Optional.empty();
    }

    /**
     * The most that can be requested from this product right now: 0 if it is restricted, otherwise 90% of the
     * available balance, rounded DOWN to the cent so rounding can never let a withdrawal exceed the limit.
     */
    public BigDecimal maxWithdrawalAmount(Product product, LocalDate today) {
        if (restrictionFor(product, today).isPresent()) {
            return ZERO_AMOUNT;
        }
        return ninetyPercentOf(product.getAvailableBalance());
    }

    /**
     * Applies every rule, throwing on the first one broken. The order decides which message the user sees:
     * eligibility first (no amount would help), then the balance, then the 90% limit.
     *
     * <p>Note: because 90% of a balance is always less than the balance itself, the 90% rule on its own
     * would also catch "more than the balance". Both are still checked, because the brief lists them as
     * separate rules and they deserve different messages: "you don't have that much" is different from
     * "you may only take 90%".
     */
    public void validate(Product product, BigDecimal amount, LocalDate today) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException(
                    RuleViolation.INVALID_AMOUNT, "Withdrawal amount must be greater than zero.");
        }

        Optional<String> restriction = restrictionFor(product, today);
        if (restriction.isPresent()) {
            throw new BusinessRuleException(RuleViolation.RETIREMENT_AGE_RESTRICTION, restriction.get());
        }

        BigDecimal available = product.getAvailableBalance();
        if (amount.compareTo(available) > 0) {
            throw new BusinessRuleException(
                    RuleViolation.INSUFFICIENT_BALANCE,
                    "Withdrawal amount of " + rand(amount) + " exceeds the available balance of " + rand(available)
                            + onHoldNote(product) + ".");
        }

        BigDecimal limit = ninetyPercentOf(available);
        if (amount.compareTo(limit) > 0) {
            throw new BusinessRuleException(
                    RuleViolation.EXCEEDS_WITHDRAWAL_LIMIT,
                    "Withdrawals may not exceed 90% of the available balance. The maximum you can withdraw is "
                            + rand(limit) + ".");
        }
    }

    private static BigDecimal ninetyPercentOf(BigDecimal balance) {
        return balance.multiply(MAX_WITHDRAWAL_RATIO).setScale(2, RoundingMode.DOWN);
    }

    // Explains why the available balance is lower than the balance, e.g. " (R 2,000.00 is on hold for open ...)".
    private static String onHoldNote(Product product) {
        if (product.getHeldAmount().signum() == 0) {
            return "";
        }
        return " (" + rand(product.getHeldAmount()) + " is on hold for open withdrawal notices)";
    }

    /** Formats an amount for user-facing messages, e.g. "R 1,250.00". Locale.ROOT keeps it the same on every server. */
    private static String rand(BigDecimal amount) {
        return String.format(Locale.ROOT, "R %,.2f", amount);
    }
}
