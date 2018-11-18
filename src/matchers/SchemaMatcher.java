package matchers;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;

/**
 * 模式层匹配器
 */
public interface SchemaMatcher {

    /**
     * 计算两个类之间的相似度
     * @param cls1
     * @param cls2
     * @return 双精度数表示相似度
     */
    double simOfClass(OntClass cls1,OntClass cls2);

    /**
     * 计算两个数据类型属性之间的相似度
     * @param pro1
     * @param pro2
     * @return 双精度数表示相似度
     */
    double simOfDp(DatatypeProperty pro1,DatatypeProperty pro2);

    /**
     * 计算两个对象属性之间的相似度
     * @param pro1
     * @param pro2
     * @return 双精度数表示相似度
     */
    double simOfOp(ObjectProperty pro1, ObjectProperty pro2);

}
