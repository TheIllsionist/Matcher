package tokenizer;

import java.util.List;

/**
 * Created by The Illsionist on 2018/11/19.
 * 字符串分词去停用词处理工具
 */
public interface Tokenizer {

    List<String> tokensOfStr(String str);  //tokens是有重复的

}
