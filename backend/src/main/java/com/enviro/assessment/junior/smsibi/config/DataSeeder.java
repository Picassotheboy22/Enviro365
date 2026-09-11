package com.enviro.assessment.junior.smsibi.config;

import com.enviro.assessment.junior.smsibi.entity.Investor;
import com.enviro.assessment.junior.smsibi.entity.Product;
import com.enviro.assessment.junior.smsibi.entity.ProductType;
import com.enviro.assessment.junior.smsibi.entity.UserAccount;
import com.enviro.assessment.junior.smsibi.entity.WithdrawalNotice;
import com.enviro.assessment.junior.smsibi.repository.InvestorRepository;
import com.enviro.assessment.junior.smsibi.repository.ProductRepository;
import com.enviro.assessment.junior.smsibi.repository.UserAccountRepository;
import com.enviro.assessment.junior.smsibi.repository.WithdrawalNoticeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads demo data into the in-memory H2 database at startup. Only active when {@code app.seed.enabled=true}, which is
 * set in the dev profile (never in production).
 *
 * <p>Dates of birth are calculated relative to today, so the demo investors stay the same age whenever the app runs.
 * The three investors cover every outcome of the retirement rule:
 * <ul>
 *   <li>Thabo, 70: eligible for retirement withdrawals;</li>
 *   <li>Sipho, exactly 65: NOT eligible (the rule is strictly "older than 65");</li>
 *   <li>Lerato, 40: NOT eligible for retirement, but can withdraw from savings.</li>
 * </ul>
 * It also creates sign-in accounts: one per investor (username = e-mail address) and one staff account, which reviews
 * and pays withdrawal notices. The demo password comes from {@code app.seed.demo-password}, so it is configuration
 * rather than code.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    public static final String STAFF_USERNAME = "admin@enviro365.example";

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository noticeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String demoPassword;

    public DataSeeder(
            InvestorRepository investorRepository,
            ProductRepository productRepository,
            WithdrawalNoticeRepository noticeRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${app.seed.demo-password}") String demoPassword) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.noticeRepository = noticeRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (investorRepository.count() > 0) {
            return; // Never seed on top of existing data.
        }
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        Investor thabo = investorRepository.save(new Investor(
                "Thabo",
                "Mokoena",
                "thabo.mokoena@example.com",
                "+27 82 555 0101",
                today.minusYears(70).minusMonths(4)));
        Investor sipho = investorRepository.save(new Investor(
                "Sipho",
                "Ndlovu",
                "sipho.ndlovu@example.com",
                "+27 83 555 0102",
                today.minusYears(65).minusMonths(2)));
        Investor lerato = investorRepository.save(new Investor(
                "Lerato",
                "Dlamini",
                "lerato.dlamini@example.com",
                "+27 84 555 0103",
                today.minusYears(40).minusMonths(7)));

        Product thaboRetirement = product(thabo, "Retirement Annuity", ProductType.RETIREMENT, "850000.00");
        Product thaboSavings = product(thabo, "Tax-Free Savings Account", ProductType.SAVINGS, "120000.00");
        product(sipho, "Preservation Fund", ProductType.RETIREMENT, "540000.00");
        Product siphoSavings = product(sipho, "Money Market Fund", ProductType.SAVINGS, "60000.00");
        product(lerato, "Retirement Annuity", ProductType.RETIREMENT, "310000.00");
        Product leratoSavings = product(lerato, "Unit Trust Portfolio", ProductType.SAVINGS, "45500.00");

        // A little history in every state of the workflow, so the tables, filters, dashboard and CSV export have
        // something to show on first run. Oldest first, so the balances add up in order.
        paid(thaboRetirement, "25000.00", now.minusDays(62));
        paid(leratoSavings, "5000.00", now.minusDays(45));
        paid(thaboSavings, "10000.00", now.minusDays(30));
        submitted(thaboSavings, "4000.00", now.minusDays(20))
                .cancel(now.minusDays(20).plusHours(3));
        paid(siphoSavings, "7500.00", now.minusDays(14));
        submitted(siphoSavings, "3000.00", now.minusDays(10))
                .reject(
                        STAFF_USERNAME,
                        now.minusDays(9),
                        "We could not verify the bank account details on file. Please contact us before submitting"
                                + " again.");
        submitted(thaboRetirement, "15000.00", now.minusDays(7)).approve(STAFF_USERNAME, now.minusDays(6));
        submitted(leratoSavings, "2000.00", now.minusDays(2)); // still pending

        // encode() is called once per account: BCrypt generates a new random salt each time, so identical demo
        // passwords still produce different hashes.
        userAccountRepository.saveAll(List.of(
                UserAccount.forInvestor(thabo, passwordEncoder.encode(demoPassword)),
                UserAccount.forInvestor(sipho, passwordEncoder.encode(demoPassword)),
                UserAccount.forInvestor(lerato, passwordEncoder.encode(demoPassword)),
                UserAccount.staff(STAFF_USERNAME, "Enviro365 Admin", passwordEncoder.encode(demoPassword))));

        log.info(
                "Seeded demo data: {} investors, {} products, {} withdrawal notices, {} user accounts",
                investorRepository.count(),
                productRepository.count(),
                noticeRepository.count(),
                userAccountRepository.count());
    }

    private Product product(Investor investor, String name, ProductType type, String balance) {
        return productRepository.save(new Product(investor, name, type, new BigDecimal(balance)));
    }

    private WithdrawalNotice submitted(Product product, String amount, LocalDateTime when) {
        return noticeRepository.save(WithdrawalNotice.submit(product, new BigDecimal(amount), when));
    }

    // Submitted, approved a few hours later and paid the next day.
    private void paid(Product product, String amount, LocalDateTime when) {
        WithdrawalNotice notice = submitted(product, amount, when);
        notice.approve(STAFF_USERNAME, when.plusHours(4));
        notice.markPaid(STAFF_USERNAME, when.plusDays(1));
    }
}
