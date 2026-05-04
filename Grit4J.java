public class Grit4J {
    @RateLimit(capacity = 5,refillTokens = 3, perSeconds = 5)
        public void handleRequest(String userID){
            System.out.println("Handling request for user: " + userID);
        }
    
    public static void main(String[] args) throws NoSuchMethodException,InterruptedException{
        Grit4J service = new Grit4J();
        RateLimitEnforcer enforcer = new RateLimitEnforcer();
        for(int i=0;i<5;i++){
            Decision decision = enforcer.enforce(service,"user-1","handleRequest",String.class);
            switch (decision) {
                case Decision.Allowed a ->
                    System.out.println("Request " + i + "-> allowed || Tokens Left " + a.tokensRemaining() );
                case Decision.Throttled t ->
                     System.out.println("Request " + i + "-> throttled || Rate Limit reset in " + t.retryAfterMs());
            }
        }

        Thread.sleep(5000);
        System.out.println("---After Refill---");
        for(int i=0;i<5;i++){
            Decision decision = enforcer.enforce(service,"user-1","handleRequest",String.class);
            System.out.println("request " + i + "-> ");
            switch(decision){
                case Decision.Allowed a ->
                    System.out.println("allowed || Tokens left " + a.tokensRemaining());
                
                case Decision.Throttled t ->
                System.out.println("throttled || Rate limit reset in " + t.retryAfterMs());
            }
        }
        
    }
}