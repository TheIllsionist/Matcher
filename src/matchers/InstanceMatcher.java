package matchers;

import org.apache.jena.ontology.Individual;

/**
 * 实例层匹配器
 */
public interface InstanceMatcher {

    /**
     * 计算两个实例之间的相似度
     * @param ins1
     * @param ins2
     * @return 双精度数表示相似度
     */
    double simOfIns(Individual ins1,Individual ins2);

}
