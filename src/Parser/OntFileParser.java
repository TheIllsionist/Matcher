package Parser;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import java.util.*;

public class OntFileParser implements OntParser{

    private Map<OntClass,List<OntProperty>> propsOfClses = null;  //缓存 类所拥有的属性集
    private Map<OntProperty,List<OntClass>> clsesOfProps = null;  //缓存 拥有当前属性的类集

    public OntFileParser(){
        propsOfClses = new HashMap<>();
        clsesOfProps = new HashMap<>();
    }

    @Override
    public List<String> labelsOf(OntResource ontRes){
        Iterator<RDFNode> lbNodes = ontRes.listLabels(null);  //不加任何语言标记限制
        List<String> labels = new ArrayList<>();
        while(lbNodes.hasNext()){
            labels.add(lbNodes.next().toString());
        }
        return labels;
    }

    @Override
    public List<String> commentsOf(OntResource ontRes){
        Iterator<RDFNode> cmNodes = ontRes.listComments(null);  //不加任何语言标记限制
        List<String> comments = new ArrayList<>();
        while(cmNodes.hasNext()){
            comments.add(cmNodes.next().toString());
        }
        return comments;
    }

    @Override
    public List<OntClass> subClassesOf(OntClass cls){
        Iterator<OntClass> iter = cls.listSubClasses(true);  //cls的直接子类
        List<OntClass> subClasses = new ArrayList<>();
        while(iter.hasNext()){
            subClasses.add(iter.next());
        }
        return subClasses;
    }

    @Override
    public List<OntClass> supClassesOf(OntClass cls) {
        Iterator<OntClass> iter = cls.listSuperClasses(true); //cls的直接父类
        List<OntClass> supClasses = new ArrayList<>();
        while(iter.hasNext()){
            supClasses.add(iter.next());
        }
        return supClasses;
    }

    @Override
    public List<OntProperty> subPropsOf(OntProperty ontProperty) {
        ExtendedIterator<? extends OntProperty> iter = ontProperty.listSubProperties(true);  //直接子属性
        List<OntProperty> subProps = new ArrayList<>();
        while(iter.hasNext()){
            subProps.add(iter.next());
        }
        return subProps;
    }

    @Override
    public List<OntProperty> supPropsOf(OntProperty ontProperty) {
        ExtendedIterator<? extends OntProperty> iter = ontProperty.listSuperProperties(true);  //直接父属性
        List<OntProperty> supProps = new ArrayList<>();
        while(iter.hasNext()){
            supProps.add(iter.next());
        }
        return supProps;
    }

    @Override
    public List<Individual> instancesOf(OntClass cls) {
        Iterator<? extends OntResource> iter =  cls.listInstances(true);  //cls的直接实例
        List<Individual> individuals = new ArrayList<>();
        while(iter.hasNext()){
            individuals.add(iter.next().asIndividual());
        }
        return individuals;
    }

    /**
     * 先从domain声明中提取类所拥有的属性信息
     * 再从该类的实例所拥有的属性中寻找
     * @param ontModel
     * @param ontClass
     * @return
     */
    @Override
    public List<OntProperty> propsOfCls(OntModel ontModel, OntClass ontClass) {
        if(propsOfClses.get(ontClass) != null){  //先尝试从缓存中取
            return propsOfClses.get(ontClass);
        }
        Set<OntProperty> propSet = new HashSet<>();
        List<OntProperty> properties = new ArrayList<>();
        //先从domain声明中寻找
        Iterator<OntProperty> props = ontModel.listOntProperties();  //迭代模型中的所有属性,寻找将OntClass作为domain的属性
        while(props.hasNext()){
            OntProperty tProp = props.next();
            if(tProp.hasProperty(RDFS.domain,ontClass)){  //当前属性的domain声明中包括该类
                propSet.add(tProp);
            }else{
                continue;
            }
        }
        //接着从实例的属性中寻找
        List<Individual> individuals = instancesOf(ontClass);
        if(individuals.size() != 0){  //TODO:目前认为如果某个类没有实例,则它不拥有任何属性
            for(Individual individual : individuals){  //遍历该类的所有实例
                StmtIterator iterator = individual.listProperties(); //遍历该实例的所有属性
                while(iterator.hasNext()){
                    propSet.add(ontModel.getOntProperty(iterator.nextStatement().getPredicate().getURI()));
                }
            }
        }
        Iterator<OntProperty> iter = propSet.iterator();
        while(iter.hasNext()){
            properties.add(iter.next());
        }
        propsOfClses.put(ontClass,properties);  //结果放入缓存
        return properties;
    }

    /**
     * 先从domain声明中寻找domain
     * 再从实例层数据中寻找,如果一个实例有这个属性,则该实例所属的类
     * @param ontModel
     * @param ontProperty
     * @return
     */
    @Override
    public List<OntClass> domainOf(OntModel ontModel, OntProperty ontProperty) {
        if(clsesOfProps.get(ontProperty) != null){
            return clsesOfProps.get(ontProperty);
        }
        Set<OntClass> clsSet = new HashSet<>();
        List<OntClass> domains = new ArrayList<>();
        //从domain声明中获取拥有该属性的类
        ExtendedIterator<? extends OntResource> dmIter = ontProperty.listDomain();
        while(dmIter.hasNext()){
            clsSet.add(dmIter.next().asClass());
        }
        //从实例层数据中获取拥有该属性的类(拥有该属性的实例的直接所属类)
        SimpleSelector selector = new SimpleSelector(null, ontProperty, (RDFNode)null); //属性作为谓词的筛选条件
        StmtIterator iterator = ontModel.listStatements(selector);
        while(iterator.hasNext()){
            Statement statement = iterator.nextStatement();
            Resource resource = statement.getSubject();
            if(resource.hasProperty(RDF.type, OWL2.NamedIndividual)){
                Individual individual = ontModel.getIndividual(resource.getURI());
                ExtendedIterator<OntClass> iClsIter = individual.listOntClasses(true);
                while(iClsIter.hasNext()){
                    clsSet.add(iClsIter.next());
                }
            }
        }
        Iterator<OntClass> iter = clsSet.iterator();
        while(iter.hasNext()){
            domains.add(iter.next());
        }
        clsesOfProps.put(ontProperty,domains);
        return domains;
    }

    /**
     * 数据类型属性的值域是字符串,对象属性的值域....(有待取出来)
     * TODO:怎么表示值域是xsd:string ? 带有中间结点的对象属性怎么取出来 ?
     * @param ontProperty
     * @return
     */
    @Override
    public List<OntResource> rangeOf(OntProperty ontProperty) {
        return null;
    }

}
