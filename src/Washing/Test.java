package Washing;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by The Illsionist on 2018/12/26.
 */
public class Test {

    public static void main(String args[]){
        String sourcePath = "G:\\wgbq_20181226.owl";
        String targetPath = "G:\\wgbq_20181226new.owl";
        String misMathFile = "G:\\misMatch.txt";
        String matchFile = "G:\\match.txt";
        File file = new File(sourcePath);
        PrintWriter misMatchWriter = null;
        PrintWriter matchWriter = null;
        try {
            misMatchWriter = new PrintWriter(new FileWriter(new File(misMathFile)));
            matchWriter = new PrintWriter(new FileWriter(new File(matchFile)));
        }catch (Exception e){
            e.printStackTrace();
        }
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontModel.read(FileManager.get().open(file.getAbsolutePath()),null);  //将owl文件内容读取到内存中
        WashTool washTool = new WashTool(misMatchWriter,matchWriter);
        ExtendedIterator<Individual> iter = ontModel.listIndividuals();
        while(iter.hasNext()){
            Individual individual = iter.next();
            washTool.washingDpVals(individual);
        }
        misMatchWriter.flush();
        matchWriter.flush();
        //把最新的数据写入一个新的文件中
        try {
            ontModel.write(new PrintWriter(new FileWriter(new File(targetPath))));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
