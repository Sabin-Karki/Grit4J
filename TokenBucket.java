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

    public TokenBucket(long capacity, long refillRateInMillis, long refillAmount,RefillStrategy refillStrategy) {
        this.capacity = capacity;
        this.refillRateInMillis = refillRateInMillis;
        this.refillAmount = refillAmount;
        this.refillStrategy = refillStrategy;
        this.tokens = new AtomicLong(capacity);
        this.stampedLock=new StampedLock();
        this.nextRefillTime=new AtomicLong(System.currentTimeMillis() + millisPerToken()); 
    }

    private Long millisPerToken() {
        return Math.max(1L,refillRateInMillis/refillAmount);
    }

    /* the rule is 2 token per second*/
    private  void refillIfNeeded(long now) {

        long scheduled = nextRefillTime.get();
        if(now<scheduled) return;

        long stamp = stampedLock.writeLock();
        try{
            now = System.currentTimeMillis();
            scheduled = nextRefillTime.get();
            if(now<scheduled) return;

            long tokensInterval = millisPerToken();
            long tokensToAdd = ((now-scheduled)/tokensInterval)+1;
            long currentTokens = tokens.get();
            long newTokenCount = Math.min(capacity,currentTokens+tokensToAdd);
            tokens.set(newTokenCount);
            long newNextRefill = scheduled + tokensToAdd*tokensInterval;
            nextRefillTime.set(newNextRefill); 
        }finally{
            stampedLock.unlockWrite(stamp);
        }
    }

    private long retryAfterMs() {
        long now = System.currentTimeMillis();
        long wait = nextRefillTime.get() - now; // this works because 
        return Math.max(wait,0);
    }
    

    public boolean tryConsume() {
        refillIfNeeded(System.currentTimeMillis());
        while(true){
        long current = tokens.get();
        if (current <= 0 ) return false;
        if (tokens.compareAndSet(current, current - 1)) return true; //tokens is set to current-1 if the currentValue of tokens = current and it also returns true  ,else it returns false

        }
    }

    public Decision check(){
        refillIfNeeded(System.currentTimeMillis());
        while(true){
            long current = tokens.get();
           if(current<=0) return new Decision.Throttled(retryAfterMs());
           if (tokens.compareAndSet(current, current-1)) return new Decision.Allowed(current-1);
        }
    }
}
