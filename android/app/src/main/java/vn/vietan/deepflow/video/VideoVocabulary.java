package vn.vietan.deepflow.video;
import java.util.*;
import java.util.regex.*;

/** Chỉ đếm văn bản của các cue SRT/VTT, không đoán họ từ hoặc trình độ đã thuộc. */
public final class VideoVocabulary {
 private static final Pattern WORDS=Pattern.compile("\\p{L}+(?:['’\\-]\\p{L}+)*");
 public final int total;
 public final Map<String,Integer> counts;
 private VideoVocabulary(int total,Map<String,Integer> counts){this.total=total;this.counts=counts;}
 public static VideoVocabulary parse(String captions){
  Map<String,Integer> counts=new HashMap<>();int total=0;
  for(String block:captions.replace("\r","").replace("\uFEFF","").split("\n\\s*\n")){
   if(block.matches("(?s)^\\s*(NOTE|STYLE|REGION)\\b.*"))continue;
   String[] lines=block.split("\n");int start=-1;for(int i=0;i<lines.length;i++)if(lines[i].contains("-->")){start=i+1;break;}if(start<0)continue;
   for(int i=start;i<lines.length;i++){
    String text=lines[i].replaceAll("<[^>]*>"," ").replaceAll("&(?:[a-zA-Z]+|#\\d+|#x[0-9A-Fa-f]+);"," ");
    Matcher m=WORDS.matcher(text);
    while(m.find()){String w=m.group().toLowerCase(Locale.ROOT);counts.put(w,counts.getOrDefault(w,0)+1);total++;}
   }
  }
  List<Map.Entry<String,Integer>> rows=new ArrayList<>(counts.entrySet());rows.sort((a,b)->a.getValue().equals(b.getValue())?a.getKey().compareTo(b.getKey()):Integer.compare(b.getValue(),a.getValue()));
  Map<String,Integer> sorted=new LinkedHashMap<>();for(Map.Entry<String,Integer> row:rows)sorted.put(row.getKey(),row.getValue());return new VideoVocabulary(total,Collections.unmodifiableMap(sorted));
 }
}
