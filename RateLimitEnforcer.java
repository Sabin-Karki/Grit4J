import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitEnforcer {
    private final ConcurrentHashMap<String,TokenBucket> buckets= new ConcurrentHashMap<>();

    public Decision enforce(Object target,String userId,String methodName,Class<?>... paramTypes)throws NoSuchMethodException{
        Method method = target.getClass().getMethod(methodName, paramTypes);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        //if no annotation ,then user request are unlimited
        if(annotation == null) return new Decision.Allowed(Long.MAX_VALUE);
        String key = target.getClass().getName() + "#" + methodName + userId;
        TokenBucket bucket = buckets.computeIfAbsent(key,k->new TokenBucket(annotation.capacity(),annotation.perSeconds()*1000L,annotation.refillTokens(),RefillStrategy.INTERVAL));
        return bucket.check();
    }
}