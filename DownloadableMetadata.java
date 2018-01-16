import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
 */
class DownloadableMetadata {
    private final String metadataFilename;
    private String filename;
    private String url;
    private File mdf;
    private long size;
    private List<Range> rangeList;
    private List<Range>  conserverRangeList;
    private int downloaded;

    DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        this.size = contentSize();
        this.mdf = getMDF();
        this.rangeList = makeRangeList();
        this.conserverRangeList = new ArrayList(this.rangeList);
        this.downloaded = 100 - this.rangeList.size();
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

    public void addDataToDynamicMetadata(Chunk chunk){
        int i = this.rangeList.indexOf(chunk.getRange());
        Range oldRange = this.conserverRangeList.get(i);
        long newStart = oldRange.getStart() + chunk.getSize_in_bytes();
        long newEnd = oldRange.getEnd();
        long dist = newStart - newEnd;
        Range newRange = new Range(newStart, newEnd);

        if(dist == 1 || dist == 0 ){
            this.downloaded++;
            System.err.println("Downloaded " + this.downloaded + "%");
        }

        // print download complete, delete mdf
        if (this.isCompleted()){
            System.err.println("Download succeeded");
            this.delete();
        } else {
            this.conserverRangeList.set(i, newRange);
            writeNewRangeToTemp();
        }
    }

    private void writeNewRangeToTemp(){
        try {
            File tempMDF = new File(this.metadataFilename + ".tmp");

            if (!tempMDF.exists()) {
                tempMDF.createNewFile();
            }

            // write current ranges to temp file
            RandomAccessFile ratmp = new RandomAccessFile(tempMDF, "rw");
            StringBuilder stringBuilder = new StringBuilder() ;
            for (Range range: this.conserverRangeList) {
                long start = range.getStart();
                long end = range.getEnd();
                long dist = start - end;

                if(!(dist == 1 || dist == 0)){
                    stringBuilder.append(Long.toString(start) + ',' + Long.toString(end) + "\n");
                }
            }
            ratmp.writeBytes(stringBuilder.toString());
            ratmp.close();
//            tempMDF.renameTo(this.mdf);
            Files.move(tempMDF.toPath(),this.mdf.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { }
    }

    String getFilename() {
        return filename;
    }

    boolean isCompleted() {
        return (this.downloaded == 100);
    }

    void delete() {
        if(this.mdf.exists()) {
            System.err.println("Deleting metadata file");

            try {
                Files.delete(this.mdf.toPath());
                System.err.println("Metadata file deleted");
            } catch (IOException e) {
                System.err.println("Metadata deletion failed");
                e.printStackTrace();
            }

        }
    }
}
