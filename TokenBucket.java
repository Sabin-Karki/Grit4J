import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

public class TokenBucket {
    private final long capacity;
    private final AtomicLong tokens;
    private final long refillRateInMillis;
    private final long refillAmount;
    private final RefillStrategy refillStrategy;
    private final StampedLock stampedLock;
    private final AtomicLong nextRefillTime;

    public TokenBucket(long capacity, long refillRateInMillis, long refillAmount, RefillStrategy refillStrategy) {
        /* validation checks */
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than 0");
        }
        if (refillRateInMillis <= 0) {
            throw new IllegalArgumentException("refillRateInMillis must be greater than 0");
        }
        if (refillAmount <= 0) {
            throw new IllegalArgumentException("refillAmount must be greater than 0");
        }
        if (refillStrategy == null) {
            throw new IllegalArgumentException("refillStrategy must not be null");
        }

        this.capacity = capacity;
        this.refillRateInMillis = refillRateInMillis;
        this.refillAmount = refillAmount;
        this.refillStrategy = refillStrategy;
        this.tokens = new AtomicLong(capacity);
        this.stampedLock = new StampedLock();
        long now = System.currentTimeMillis();
        this.nextRefillTime = new AtomicLong(firstRefillTime(now));
    }

    private long firstRefillTime(long now) {
        if (refillStrategy == RefillStrategy.INTERVAL) {
            return now + refillRateInMillis;
        }
        return now + millisPerToken();
    }

    private long millisPerToken() {
        return Math.max(1L, refillRateInMillis / refillAmount);
    }

    private void refillNeeded(long now) {
        long scheduled = nextRefillTime.get();
        if (now < scheduled) {
            return;
        }
        long tokensToAdd;
        long newNextRefill;

        if (refillStrategy == RefillStrategy.INTERVAL) {
            long intervalsPassed = ((now - scheduled) / refillRateInMillis) + 1;
            tokensToAdd = intervalsPassed * refillAmount;
            newNextRefill = scheduled + intervalsPassed * refillRateInMillis;
        } else {
            long tokenInterval = millisPerToken();
            tokensToAdd = ((now - scheduled) / tokenInterval) + 1;
            newNextRefill = scheduled + tokensToAdd * tokenInterval;
        }

        long currentTokens = tokens.get();
        long newTokenCount = Math.min(capacity, currentTokens + tokensToAdd);
        tokens.set(newTokenCount);
        nextRefillTime.set(newNextRefill);
    }

    private long retryAfterMs(long now) {
        long wait = nextRefillTime.get() - now;
        return Math.max(wait, 0);
    }

    private boolean tryConsumeLocked(long now) {
        refillNeeded(now);
        long current = tokens.get();
        if (current <= 0) {
            return false;
        }
        tokens.set(current - 1);
        return true;
    }

    public boolean tryConsume() {
        long stamp = stampedLock.writeLock();
        try {
            return tryConsumeLocked(System.currentTimeMillis());
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }

    public Decision check() {
        long stamp = stampedLock.writeLock();
        try {
            long now = System.currentTimeMillis();
            refillNeeded(now);
            long current = tokens.get();
            if (current <= 0) {
                return new Decision.Throttled(retryAfterMs(now));
            }
            long remaining = current - 1;
            tokens.set(remaining);
            return new Decision.Allowed(remaining);
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }
}
