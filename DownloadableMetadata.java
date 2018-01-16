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
        File mdf = new File(this.metadataFilename);
        try {
            File tempMDF = new File(this.metadataFilename + ".tmp");

            // delete .tmp file if program crashed before renaming it
            Files.deleteIfExists(tempMDF.toPath());

            if (!mdf.exists()) {
                if(mdf.createNewFile()) {
                    initMDF(mdf);
                } else {
                    System.err.println("Error creating metadata file. Download failed.");
                }
            }
            return mdf;
        } catch (IOException e) {
            System.err.println("Error getting metadata file. Download failed.");
            return mdf;
        }
    }

    private void initMDF(File mdf){
        try {

            // initialize metadata file
            RandomAccessFile ramdf = new RandomAccessFile(mdf, "rw");
            StringBuilder stringBuilder = new StringBuilder() ;
            Long start, end, percent;
            percent = this.size/100;

            // write to metadata file ranges
            // each range is a percent of the file size, apart from maybe the last
            for (long i = 0; i< 100; i++){
                start = i*percent;
                end = start + percent - 1;
                if(i == 99 && end != this.size){
                    end = this.size;
                }
                String sRange = Long.toString(start) + ',' + Long.toString(end) + "\n";
                stringBuilder.append(sRange);
            }
            ramdf.writeBytes(stringBuilder.toString());
            ramdf.close();
        } catch (IOException e) {
            System.err.println("Error initiating metadata file. Download failed.");
        }
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
            ramdf.close();
        } catch (IOException e) {
            System.err.println("Error occurred while getting ranges. Download failed.");
        }
        return rangeList;
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

        // print download complete message  , delete mdf
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
                if(!tempMDF.createNewFile()){
                    System.err.println("Error creating temporary metadata file. Download failed.");
                }
            }

            // write current ranges to temp file
            RandomAccessFile ratmp = new RandomAccessFile(tempMDF, "rw");
            StringBuilder stringBuilder = new StringBuilder() ;
            for (Range range: this.conserverRangeList) {
                long start = range.getStart();
                long end = range.getEnd();
                long dist = start - end;

                if(!(dist == 1 || dist == 0)){
                    String sRange = Long.toString(start) + ',' + Long.toString(end) + "\n";
                    stringBuilder.append(sRange);
                }
            }
            ratmp.writeBytes(stringBuilder.toString());
            ratmp.close();

            //attempt to rename .tmp file
            try {
                Files.move(tempMDF.toPath(), this.mdf.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }catch (IOException e) {
                System.err.println("Error renaming .tmp file. Download failed.");
            }
        } catch (IOException e) {
            System.err.println("Error writing to .tmp file. Download failed.");
        }
    }

    String getFilename() {
        return filename;
    }

    boolean isCompleted() {
        return (this.downloaded == 100);
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

    private void delete() {
        if(this.mdf.exists()) {
            System.err.println("Deleting metadata file");

            try {
                Files.delete(this.mdf.toPath());
                System.err.println("Metadata file deleted");
            } catch (IOException e) {
                System.err.println("Metadata deletion failed");
            }
        }
    }
}
