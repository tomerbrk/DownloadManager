import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 * NOTE: make sure that the file interface you choose writes every update to the file's content or metadata
 *       synchronously to the underlying storage device.
 */
public class FileWriter implements Runnable {

    private final BlockingQueue<Chunk> chunkQueue;
    private DownloadableMetadata downloadableMetadata;

    FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.downloadableMetadata = downloadableMetadata;
    }

    private void writeChunks() throws IOException {
        File file = new File(downloadableMetadata.getFilename());
        RandomAccessFile writer = new RandomAccessFile(file, "rw");
        while (!this.downloadableMetadata.isCompleted()) {
            try{
                Chunk chunk = chunkQueue.take();
                writer.seek(chunk.getOffset());
                writer.write(chunk.getData(),0,chunk.getSize_in_bytes());
                this.downloadableMetadata.addDataToDynamicMetadata(chunk);

            } catch (InterruptedException e) {
                System.err.println("Could not write chunk to file. Download Failed");
            }
        }
        writer.close();
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO
        }
    }
}
