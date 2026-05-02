public sealed interface Decision permits Decision.Allowed,Decision.Throttled{
    record Allowed(long tokensRemaining) implements Decision {}
    record Throttled(long retryAfterMs ) implements Decision {}
}

/** .Allowed, . Throttled are the two possible decisions that can be made by TokenBucket Method which is basically whether the request for the token is allowed or denied ,the two are sub classes/nested type of Decision interface */
