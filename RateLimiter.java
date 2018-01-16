/**
 * A token bucket based rate-limiter.
 *
 * This class should implement a "soft" rate limiter by adding maxBytesPerSecond tokens to the bucket every second,
 * or a "hard" rate limiter by resetting the bucket to maxBytesPerSecond tokens every second.
 */
public class RateLimiter implements Runnable {
    private final TokenBucket tokenBucket;
    private final Long maxBytesPerSecond;
    private DownloadableMetadata metadata;

    RateLimiter(TokenBucket tokenBucket, Long maxBytesPerSecond,DownloadableMetadata metadata) {
        this.tokenBucket = tokenBucket;
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.metadata = metadata;
    }

    @Override
    public void run() {
        while (!this.metadata.isCompleted()) {
            try{
               tokenBucket.set(maxBytesPerSecond);
               Thread.sleep(1000);

            } catch (InterruptedException e) {
                System.err.println("RateLimiter interrupted. Download failed.");
                System.exit(-1);
            }
        }
    }
}
