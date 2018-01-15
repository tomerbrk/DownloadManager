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
                long offset = range.getStart();
                while (offset < range.getEnd()) {
                    if(tokenBucket.getSize() >= CHUNK_SIZE){
                        byte[] data = new byte[CHUNK_SIZE];
                        tokenBucket.take(CHUNK_SIZE);
                        int size_in_bytes = inputStream.read(data);
                        Chunk outChunk = new Chunk(data, offset, size_in_bytes, this.range);
                        offset += size_in_bytes;
                        outQueue.put(outChunk);
                    } else {
                        continue;
                    }
                }
                inputStream.close();
                connection.disconnect();
            }

        } catch (IOException e) {
            System.err.println("Couldn't fetch range starting at :" + this.range.getStart() + " and ending at: " + this.range.getEnd());
            System.err.println("Download failed");
        }
    }

    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            //TODO
        }
    }
}
