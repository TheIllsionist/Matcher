package Washing;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.io.*;

/**
 * Created by The Illsionist on 2018/12/26.
 */
public class Test {

    public static void main(String args[]){
        String sourcePath = "G:\\wgbq_20181226.owl";
        String targetPath1 = "G:\\wgbq_20181226hasMid.owl";
//        String targetPath2 = "G:\\wgbq_20181226noMid.owl";
        String misMathFile = "G:\\misMatch.txt";
        String matchFile = "G:\\match.txt";
        File sourceFile = new File(sourcePath);
        PrintWriter misMatchWriter = null;
        PrintWriter matchWriter = null;
        try {
            misMatchWriter = new PrintWriter(new FileWriter(new File(misMathFile)));
            matchWriter = new PrintWriter(new FileWriter(new File(matchFile)));
        }catch (Exception e){
            e.printStackTrace();
        }
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontModel.read(FileManager.get().open(sourceFile.getAbsolutePath()),null);  //将owl文件内容读取到内存中
        WashTool washTool = new WashTool(misMatchWriter,matchWriter);
        ExtendedIterator<Individual> iter = ontModel.listIndividuals();
        while(iter.hasNext()){
            Individual individual = iter.next();
            washTool.washingDpVals(individual);
        }
        misMatchWriter.flush();
        matchWriter.flush();

        File folder = new File("G:\\Data");
        File[] files = folder.listFiles();
        try {
            BufferedReader reader;
            String line = null;
            for(File tmpData : files){
                reader = new BufferedReader(new FileReader(tmpData));
                line = reader.readLine().replaceAll("\\s{1,}"," ").trim();  //将一个或多个空白符替换为一个空格,然后去掉首尾空格
                String[] tR = line.split(" ");
                if(tR.length == 3){  //数据类型属性
                    washTool.addDpVal(ontModel,tR[0],tR[1],tR[2]);
                    line = reader.readLine().replaceAll("\\s{1,}"," ").trim();
                    while(line != null){
                        line = line.replaceAll("\\s{1,}"," ").trim();
                        tR = line.split(" ");
                        if(tR.length == 3){
                            washTool.addDpVal(ontModel,tR[0],tR[1],tR[2]);
                        }else{
                            System.out.println(line);
                        }
                        line = reader.readLine();
                    }
                }else{   //对象属性
                    washTool.addOpVal(ontModel,tR[0],tR[1],tR[2],tR[3]);
                    line = reader.readLine().replaceAll("\\s{1,}"," ").trim();
                    while(line != null){
                        line = line.replaceAll("\\s{1,}"," ").trim();
                        tR = line.split(" ");
                        if(tR.length == 4){
                            washTool.addOpVal(ontModel,tR[0],tR[1],tR[2],tR[3]);
                        }else{
                            System.out.println(line);
                        }
                        line = reader.readLine();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("缺失的实例:");
        for (String str: washTool.getIns1Missing()) {
            System.out.println(str);
        }
        for(String str : washTool.getIns2Missing()) {
            System.out.println(str);
        }
        System.out.println("创建的实例:");
        for(String str : washTool.getInsCreating()){
            System.out.println(str);
        }
        System.out.println("缺失的DP:");
        for(String str : washTool.getDpMissing()){
            System.out.println(str);
        }
        System.out.println("创建的DP:");
        for(String str : washTool.getDpCreating()){
            System.out.println(str);
        }
        System.out.println("缺失的OP:");
        for(String str : washTool.getOpMissing()){
            System.out.println(str);
        }
        System.out.println("创建的OP:");
        for(String str : washTool.getOpCreating()){
            System.out.println(str);
        }
        System.out.println("缺失的Cls:");
        for(String str : washTool.getClsMissing()){
            System.out.println(str);
        }
        System.out.println("创建的Cls:");
        for(String str : washTool.getClsCreating()){
            System.out.println(str);
        }
        //把最新的数据写入一个新的文件中
        try {
            ontModel.write(new PrintWriter(new FileWriter(new File(targetPath1))));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
