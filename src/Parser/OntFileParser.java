package Parser;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OntFileParser implements OntParser{

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

}
