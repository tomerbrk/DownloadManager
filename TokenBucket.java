/**
 * A Token Bucket (https://en.wikipedia.org/wiki/Token_bucket)
 *
 */
class TokenBucket {
    private long size;
    TokenBucket(long tokens) {
        this.size = tokens;
    }

    synchronized long take(long tokens) {
        if(tokens < this.size){
            this.size -= tokens;
            return tokens;
        }
        return 0;
    }

    synchronized void set(long tokens) {
        this.size = tokens;
    }
}
