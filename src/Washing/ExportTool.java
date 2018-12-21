package Washing;

import Parser.OntParser;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.io.*;
import java.util.List;

public class ExportTool {

    private OntParser ontParser = null;

    public ExportTool(OntParser ontParser){
        this.ontParser = ontParser;
    }

    /**
     * 将知识库中每个类所拥有的属性持久化到文档里
     * @param ontModel
     * @param outFile
     */
    public void exportPropsOfClses(OntModel ontModel,File outFile){
        ExtendedIterator<OntClass> clses = ontModel.listClasses();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(outFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        while(clses.hasNext()){
            OntClass tCls = clses.next();
            List<OntProperty> props = ontParser.propsOfCls(ontModel,tCls);  //得到当前类所拥有的属性
            String tmpStr = tCls.getURI() + ": \n ";   //先写入类的uri
            for (OntProperty prop : props) {
                tmpStr += "    " + prop.getURI() + "; \n"; //然后写入属性的uri
            }
            writer.print(tmpStr);
        }
        writer.flush();
    }

    /**
     * 将知识库中每个属性的定义域持久化到文档里
     * @param ontModel
     * @param outFile
     */
    public void exportClsesOfProps(OntModel ontModel,File outFile){
        ExtendedIterator<OntProperty> props = ontModel.listOntProperties();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(outFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        while(props.hasNext()){
            OntProperty tProp = props.next();
            List<OntClass> clses = ontParser.domainOf(ontModel,tProp);  //得到拥有当前属性的所有类
            String tmpStr = tProp.getURI() + ": \n ";  //先写入属性的uri
            for(OntClass cls : clses){
                tmpStr += "    " + cls.getURI() + "; \n"; //然后写入类的uri
            }
            writer.print(tmpStr);
        }
        writer.flush();
    }


}
