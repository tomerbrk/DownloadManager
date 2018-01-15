import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 *
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 *
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
class DownloadableMetadata {
    private final String metadataFilename;
    private String filename;
    private String url;
    private File mdf;
    private long size;
    private List<Range> rangeList;
    private MetaConserverList conserverList;

    DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        this.size = contentSize();
        this.mdf = getMDF();
        this.rangeList = makeRangeList();
        this.conserverList = new MetaConserverList(this.rangeList);
        // call a progress func
//        System.out.println(rangeList.size());
    }

    private File getMDF() {
        try {
            File mdf = new File(this.metadataFilename);
            if (!mdf.exists()) {
                mdf.createNewFile();
                initMDF(mdf);
            }
            return mdf;
        } catch (IOException e) {
           return null;
        }
    }

    private void initMDF(File mdf){
        try {

            // initialize metadata file
            RandomAccessFile ramdf = new RandomAccessFile(mdf, "rw");
            StringBuilder stringBuilder = new StringBuilder() ;
            Long start, end, percent;
            percent = this.size/100;
            for (long i = 0; i< 100; i++){
                start = i*percent;
                end = start + percent - 1;
                if(i == 99 && end != this.size){
                    end = this.size;
                }
                stringBuilder.append(Long.toString(start) + ',' + Long.toString(end) + "\n");
            }
            ramdf.writeBytes(stringBuilder.toString());
            ramdf.close();
        } catch (IOException e) { }
    }

    private long contentSize(){
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int res = connection.getResponseCode();
            return  (res / 100 == 2) ? connection.getContentLengthLong() : 0;

        } catch (IOException e) {
            return 0;
        }
    }

    private List<Range> makeRangeList(){
        List<Range> rangeList = new ArrayList<>();
        try {
            RandomAccessFile ramdf = new RandomAccessFile(this.mdf, "rw");
            Range range;
            String line;
            String[] separated;
            while ((line = ramdf.readLine()) != null) {
                    separated = line.split(",");
                    range = new Range(Long.parseLong(separated[0]), Long.parseLong(separated[1]));
                    rangeList.add(range);
            }
        } catch (IOException e) {}

        return rangeList;
    }

    public List<Range> getRangeList(){
        return this.rangeList;
    }

    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    public void addDataToDynamicMetadat(Chunk chunk){
        Range range = this.conserverList.addChunkToRange(chunk.getOffset(),chunk.getSize_in_bytes());
        if(range != null){
            this.removeRange(range);
        }
    }
    public void removeRange(Range range){
        this.rangeList.remove(range);
        System.out.println("Downloaded " + (100 - this.rangeList.size()) + "%");
    }

    String getFilename() {
        return filename;
    }

    boolean isCompleted() {
        return (this.rangeList.size() == 0);
    }

    void delete() {
        this.mdf.delete();
    }


    String getUrl() {
        return url;
    }
}
