import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {
    private final long capacity;
    private final AtomicLong tokens;
    private final long refillRateInMillis;
    private final long refillAmount;
    private volatile long lastRefillTime;

    public TokenBucket(long capacity, long refillRateInMillis, long refillAmount) {
        this.capacity = capacity;
        this.tokens = new AtomicLong(capacity);
        this.refillRateInMillis = refillRateInMillis;
        this.refillAmount = refillAmount;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public boolean tryConsume() {
        refill();
        long current = tokens.get();
        if (current >= 1)
        return tokens.compareAndSet(current, current - 1); // current = current -1
        return false;
    }

    /* the rule is 2 token per second,so the tokenToadd  = () */
    private void refill() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;
        if(timePassed > refillRateInMillis){
            long tokensToAdd = (timePassed/refillRateInMillis) * refillAmount; // how is this the formula?because
            //for new token count ? why is Math.min tokens.get,capacity and + tokensToADd required ,what is the logic? the logic is 
            long newTokenCount = Math.min(capacity,tokens.get()+tokensToAdd); // the why is ,firstly the capacity=10 always cause its the limit,now the tokens lets assume it has 6 tokens and tokens to add is 2 so the parameter will be 8,and because of the  Math.min, the newTokenCount= will always be the newTokenCount=8 and never be more than capacity,and will be 10 at most 
            tokens.set(newTokenCount);
            lastRefillTime = System.currentTimeMillis();
        }
    }
}
