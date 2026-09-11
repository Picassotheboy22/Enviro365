package com.enviro.assessment.junior.smsibi.exception;

import com.enviro.assessment.junior.smsibi.entity.NoticeStatus;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Thrown when a withdrawal notice cannot make the requested move, e.g. paying a notice that was never approved, or
 * cancelling one that staff have already approved.
 *
 * <p>Mapped to 409 Conflict by {@link GlobalExceptionHandler}: the request itself is fine, but it clashes with the
 * notice's current state. That usually means someone else acted on the notice first, so the UI should refresh.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final NoticeStatus currentStatus;

    public InvalidStatusTransitionException(Long noticeId, NoticeStatus currentStatus, NoticeStatus requestedStatus) {
        super(message(noticeId, currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
    }

    public NoticeStatus getCurrentStatus() {
        return currentStatus;
    }

    // E.g. "Withdrawal notice #12 is paid, so it cannot be approved. Only pending notices can be approved."
    private static String message(Long noticeId, NoticeStatus current, NoticeStatus requested) {
        String notice = noticeId == null ? "This withdrawal notice" : "Withdrawal notice #" + noticeId;
        String allowedFrom = Arrays.stream(NoticeStatus.values())
                .filter(status -> status.canMoveTo(requested))
                .map(InvalidStatusTransitionException::word)
                .collect(Collectors.joining(" or "));
        return notice + " is " + word(current) + ", so it cannot be " + word(requested) + ". Only " + allowedFrom
                + " notices can be " + word(requested) + ".";
    }

    // The status names double as English words: "pending", "approved", "paid", "rejected" and "cancelled".
    private static String word(NoticeStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
