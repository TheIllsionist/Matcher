package matchers;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.util.Map;

/**
 * 虚拟文档方法进行模式层匹配
 */
public class VDSchemaMatcher implements SchemaMatcher {

    private OntModel o1 = null; //待匹配本体1
    private OntModel o2 = null; //待匹配本体2
    private int m;  //本体1中文档数
    private int n;  //本体2中文档数
    private Map<String,Integer> bsVec = null;    //向量空间坐标基矢量(唯一单词,第几维)
    private Map<String,Integer> idfInfo = null;  //坐标基矢量中每个词的逆文档频率

    public VDSchemaMatcher(OntModel o1,OntModel o2){
        this.o1 = o1;
        this.o2 = o2;

    }

    @Override
    public double simOfClass(OntClass cls1, OntClass cls2) {
        return 0;
    }

    @Override
    public double simOfDp(DatatypeProperty pro1, DatatypeProperty pro2) {
        return 0;
    }

    @Override
    public double simOfOp(ObjectProperty pro1, ObjectProperty pro2) {
        return 0;
    }

}
