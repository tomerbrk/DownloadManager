/**
 * A Token Bucket (https://en.wikipedia.org/wiki/Token_bucket)
 *
 * This thread-safe bucket should support the following methods:
 *
 * - take(n): remove n tokens from the bucket (blocks until n tokens are available and taken)
 * - set(n): set the bucket to contain n tokens (to allow "hard" rate limiting)
 */
class TokenBucket {
    private long size;
    TokenBucket(long tokens) {
        this.size = tokens;
    }

    synchronized void take(long tokens) {
        if(tokens < this.size){
            this.size -= tokens;
        }
    }

    synchronized void set(long tokens) {
        this.size = tokens;
    }

    synchronized long getSize(){
        return this.size;
    }
}
