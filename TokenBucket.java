import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

public class TokenBucket {
    private final long capacity;
    private final AtomicLong tokens;
    private final long refillRateInMillis;
    private final long refillAmount;
    private final ScheduledExecutorService scheduler;
    private final RefillStrategy refillStrategy;
    private final StampedLock stampedLock;

    public TokenBucket(long capacity, long refillRateInMillis, long refillAmount,RefillStrategy refillStrategy) {
        this.capacity = capacity;
        this.refillRateInMillis = refillRateInMillis;
        this.refillAmount = refillAmount;
        this.refillStrategy = refillStrategy;
        this.tokens = new AtomicLong(capacity);
        this.stampedLock=new StampedLock();

        this.scheduler=Executors.newSingleThreadScheduledExecutor(runnable->{
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        });
    }
    /* the rule is 2 token per second,so the tokenToadd  = () */
    private void refill() {
        
        long stamp = stampedLock.writeLock();
        try{
            long tokensToAdd = (refillStrategy == RefillStrategy.INTERVAL) ? refillAmount : capacity;    
            tokens.set(Math.min(capacity,tokens.get()+tokensToAdd)); // the why is ,firstly the capacity=10 always cause its the limit,now the tokens lets assume it has 6 tokens and tokens to add is 2 so the parameter will be 8,and because of the  Math.min, the newTokenCount= will always be the newTokenCount=8 and never be more than capacity,and will be 10 at most 
        }finally{
            stampedLock.unlockWrite(stamp);
        }
    }

    public boolean tryConsume() {

        while(true){
        long current = tokens.get();
        if (current <= 0 ) return false;
        if (tokens.compareAndSet(current, current - 1)) return true; //tokens is set to current-1 if the currentValue of tokens = current and it also returns true  ,else it returns false

        }
    }

    public Decision check(){
        boolean consumed = tryConsume();
        if(consumed){
            return new Decision.Allowed(tokens.get());
        }
        return new Decision.Throttled(refillRateInMillis);
    }

}
