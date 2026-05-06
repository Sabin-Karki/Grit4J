import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitEnforcer {
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public Decision enforce(Object target, String userId, String methodName, Object... args)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }

        Method method = target.getClass().getMethod(methodName, paramTypes);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        if (annotation == null) {
            method.invoke(target, args);
            return new Decision.Allowed(Long.MAX_VALUE);
        }

        String key = target.getClass().getName() + "#" + methodName + "#" +  userId;
        TokenBucket bucket = buckets.computeIfAbsent(
                key,
                k -> new TokenBucket(
                        annotation.capacity(),
                        annotation.perSeconds() * 1000L,
                        annotation.refillTokens(),
                        RefillStrategy.GREEDY));

        Decision decision = bucket.check();
        if (decision instanceof Decision.Allowed) {
            method.invoke(target, args);
        }
        return decision;
    }
}
