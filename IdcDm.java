import java.util.concurrent.*;

public class IdcDm {

    /**
     * Receive arguments from the command-line, provide some feedback and start the download.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int numberOfWorkers = 1;
        Long maxBytesPerSecond = null;

        if (args.length < 1 || args.length > 3) {
            System.err.printf("usage:\n\tjava IdcDm URL [MAX-CONCURRENT-CONNECTIONS] [MAX-DOWNLOAD-LIMIT]\n");
            System.exit(1);
        } else if (args.length >= 2) {
            numberOfWorkers = Integer.parseInt(args[1]);
            if (args.length == 3)
                maxBytesPerSecond = Long.parseLong(args[2]);
        }

        String url = args[0];

        System.err.printf("Downloading");
        if (numberOfWorkers > 1)
            System.err.printf(" using %d connections", numberOfWorkers);
        if (maxBytesPerSecond != null)
            System.err.printf(" limited to %d Bps", maxBytesPerSecond);
        System.err.printf("...\n");

        DownloadURL(url, numberOfWorkers, maxBytesPerSecond);
    }

    /**
     * Initiate the file's metadata, and iterate over missing ranges. For each:
     * 1. Setup the Queue, TokenBucket, DownloadableMetadata, FileWriter, RateLimiter, and a pool of HTTPRangeGetters
     * 2. Join the HTTPRangeGetters, send finish marker to the Queue and terminate the TokenBucket
     * 3. Join the FileWriter and RateLimiter
     *
     * Finally, print "Download succeeded/failed" and delete the metadata as needed.
     *
     * @param url URL to download
     * @param numberOfWorkers number of concurrent connections
     * @param maxBytesPerSecond limit on download bytes-per-second
     */
    private static void DownloadURL(String url, int numberOfWorkers, Long maxBytesPerSecond) {
        maxBytesPerSecond = (maxBytesPerSecond != null) ? maxBytesPerSecond : Long.MAX_VALUE;
        BlockingQueue<Chunk> outQueue = new LinkedBlockingQueue<>();
        TokenBucket tokenBucket = new TokenBucket(maxBytesPerSecond);
        DownloadableMetadata metadata = new DownloadableMetadata(url);
        FileWriter fileWriter = new FileWriter(metadata, outQueue);
        RateLimiter rateLimiter = new RateLimiter(tokenBucket, maxBytesPerSecond, metadata);
        Thread rateLimiterThread = new Thread(rateLimiter);
        Thread fileWriterThread = new Thread(fileWriter);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);

        try {
            rateLimiterThread.start();
            fileWriterThread.start();
            for (Range range: metadata.getRangeList()) {
                Runnable worker = new HTTPRangeGetter(url,range, outQueue, tokenBucket);
                executor.execute(worker);
            }
            executor.shutdown();
        } catch (Exception e){
            System.err.println("Error in main function. Download failed.");
            System.exit(-1);
        }
    }
}
