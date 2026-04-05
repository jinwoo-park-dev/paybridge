package com.paybridge.providers.nicepay;

import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NicePayTidGenerator {

    private static final ZoneId NICEPAY_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final DateTimeFormatter EDI_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SecureRandom secureRandom;
    private final Clock clock;

    public NicePayTidGenerator() {
        this(Clock.system(NICEPAY_ZONE), new SecureRandom());
    }

    NicePayTidGenerator(Clock clock, SecureRandom secureRandom) {
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public String nextCreditCardTid(String merchantId) {
        if (merchantId == null || merchantId.length() != 10) {
            throw new PayBridgeException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "NicePay MID must be exactly 10 characters.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        return merchantId
                + "01"
                + "01"
                + now.format(TID_TIME_FORMAT)
                + String.format("%04d", secureRandom.nextInt(10_000));
    }

    public String nextEdiDate() {
        return LocalDateTime.now(clock).format(EDI_TIME_FORMAT);
    }
}
