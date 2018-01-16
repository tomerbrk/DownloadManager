import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * A runnable class which downloads a given url.
 * It reads CHUNK_SIZE at a time and writs it into a BlockingQueue.
 * It supports downloading a range of data, and limiting the download rate using a token bucket.
 */
public class HTTPRangeGetter implements Runnable {
    static final int CHUNK_SIZE = 4096;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    private final String url;
    private final Range range;
    private final BlockingQueue<Chunk> outQueue;
    private TokenBucket tokenBucket;

    HTTPRangeGetter(
            String url,
            Range range,
            BlockingQueue<Chunk> outQueue,
            TokenBucket tokenBucket) {
        this.url = url;
        this.range = range;
        this.outQueue = outQueue;
        this.tokenBucket = tokenBucket;
    }

    private void downloadRange() throws IOException, InterruptedException {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // set connection props
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Range", "bytes=" + this.range.getStart() + "-" + this.range.getEnd());


            if  (connection.getResponseCode() / 100 == 2){
                InputStream inputStream = connection.getInputStream();
                long readBytes = 0;
                long bytesToRead = range.getLength();
                while (readBytes < bytesToRead) {
                    if(tokenBucket.take(CHUNK_SIZE) == CHUNK_SIZE){
                        byte[] data = new byte[CHUNK_SIZE];
                        long offset = range.getStart() + readBytes;
                        int size_in_bytes = inputStream.read(data);

                        // check for EOF
                        if(size_in_bytes == -1){
                            break;
                        }

                        readBytes += size_in_bytes;
                        Chunk outChunk = new Chunk(data, offset, size_in_bytes, this.range);
                        outQueue.put(outChunk);
                    }
                }
                inputStream.close();
                connection.disconnect();
            }

        } catch (IOException e) {
            String err = "Couldn't fetch range starting at :" + this.range.getStart() + " and ending at: " + this.range.getEnd() + ". Download failed.";
            throw new IOException(err);
        } catch (InterruptedException e ){
            String err = "Runtime interruption. Download failed.";
            throw new InterruptedException(err);
        }
    }

    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }
}
