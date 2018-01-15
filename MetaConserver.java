import java.util.ArrayList;
import java.util.List;

public class MetaConserver {
    private List<Range> dataBucket;
    private Range range;

    MetaConserver(Range range){
        this.range = range;
        this.dataBucket = new ArrayList<>();
    }


    public boolean addRange(long offset, long length){
        Range range;
        for (int i = 0; i < this.dataBucket.size();i++ ){
            long start = this.dataBucket.get(i).getStart();
            long end = this.dataBucket.get(i).getEnd();

            if (start == (offset + length)) {
                range = new Range(offset, start);
                this.dataBucket.set(i, range);
                break;
            }
            else if (end == offset - 1){
                range = new Range(start, offset);
                this.dataBucket.set(i, range);
                break;
            }
        }
        range = new Range(offset, offset + (length -1));
        this.dataBucket.add(range);

        return this.isRangeComplete();
    }

    private boolean isRangeComplete(){
        return (this.dataBucket.size() == 1 &&
                this.dataBucket.get(0).getStart() == this.range.getStart() &&
                this.dataBucket.get(0).getEnd() == this.range.getStart());
    }

    public Range getRange(){
        return this.range;
    }

    public long getStart(){
        return this.range.getStart();
    }

    public long getEnd(){
        return this.range.getEnd();
    }
}
