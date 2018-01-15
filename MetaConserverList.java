import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MetaConserverList {
    private List<MetaConserver>  conserverList;

    MetaConserverList(List<Range> rangeList){
        this.conserverList = new ArrayList<>();
        for (Range range: rangeList) {
            MetaConserver metaConserver = new MetaConserver(range);
            this.conserverList.add(metaConserver);
        }
    }

    public Range addChunkToRange(long offset, long length){
        for (MetaConserver conserver: this.conserverList) {
          if(conserver.getStart() <= offset && conserver.getEnd() >= (offset + length - 1 )){
              return (conserver.addRange(offset,length)) ? conserver.getRange() : null;
          }
        }
        System.out.println("range not found..??");
        return null;
    }
}
