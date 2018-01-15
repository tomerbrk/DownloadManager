import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 */
public class FileWriter implements Runnable {

    private final BlockingQueue<Chunk> chunkQueue;
    private DownloadableMetadata metadata;

    FileWriter(DownloadableMetadata metadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.metadata = metadata;
    }

    private void writeChunks() throws IOException, InterruptedException {
        File file = new File(metadata.getFilename());
        RandomAccessFile writer = new RandomAccessFile(file, "rw");
        while (!this.metadata.isCompleted()) {
            try{
                Chunk chunk = chunkQueue.take();
                writer.seek(chunk.getOffset());
                writer.write(chunk.getData(),0,chunk.getSize_in_bytes());
                this.metadata.addDataToDynamicMetadata(chunk);

            } catch (IOException e) {
                throw new IOException("Could not write chunk to file. Download Failed");
            } catch (InterruptedException e) {
                String err = "Runtime interruption. Download failed.";
                throw new InterruptedException(err);
            }
        }
        writer.close();
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
        }
    }
}
