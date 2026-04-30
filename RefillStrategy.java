public enum RefillStrategy {
    /*
     * Defines how the tokens will refill into the "bucket"
     * greedy->after 0.5 second at 2token/sec, while interval->token arrive in bulk
     * at end of each interval
     */
    GREEDY,
    INTERVAL
}
