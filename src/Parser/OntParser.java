package Parser;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import java.util.List;

/**
 * 本体解析接口
 * 提供一个操作本体知识库(文件,Triple Store DB等等)的统一接口
 */
public interface OntParser {
    /**
     * 返回一个本体资源的所有可读名称
     * @param resource
     * @return
     */
    List<String> labelsOf(OntResource resource);

    /**
     * 返回一个本体资源的所有解释说明
     * @param resource
     * @return
     */
    List<String> commentsOf(OntResource resource);

    /**
     * 返回一个本体类的所有直接子类
     * @param ontClass
     * @return
     */
    List<OntClass> subClassesOf(OntClass ontClass);

    /**
     * 返回一个本体类的所有直接父类
     * @param ontClass
     * @return
     */
    List<OntClass> supClassesOf(OntClass ontClass);

    /**
     * 返回一个本体属性的所有直接子属性
     * @param ontProperty
     * @return
     */
    List<OntProperty> subPropsOf(OntProperty ontProperty);

    /**
     * 返回一个本体属性的所有直接父属性
     * @param ontProperty
     * @return
     */
    List<OntProperty> supPropsOf(OntProperty ontProperty);

    /**
     * 返回一个本体类的所有实例(直接实例)
     * @param ontClass
     * @return
     */
    List<Individual> instancesOf(OntClass ontClass);
}
