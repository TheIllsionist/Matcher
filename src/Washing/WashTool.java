package Washing;

import Parser.OntParser;
import org.apache.jena.ontology.*;
import org.apache.jena.vocabulary.RDF;

import java.util.HashMap;
import java.util.Map;

public class WashTool {

    private OntParser ontParser = null;
    private Map<String[],String> nfToMf = null;//属性名特征与提取特征对,当遇到一个属性与属性名特征中的一个匹配时,便按提取特征处理
    private Map<String,String> nsMap = null;

    private void initNfToMf(){
        nfToMf = new HashMap<>();
        nfToMf.put(new String[]{"时间","年限","日期"},
                "\\d{4}年(\\d{1,2}月)?(\\d{1,2}日)?(\\d{1,2}(点|时))?(\\d{1,2}分)?(\\d{1,2}秒)?");
        nfToMf.put(new String[]{"速","速度"},"\\d+(，|,)?\\d*\\.?\\d*(公里/小时|千米/小时|发/分|米/秒|千米/时|千米每小时|节)$");
        nfToMf.put(new String[]{"径","翼展","航程","射程","宽","宽度","长度","长","高度","深度","行程","高"},"\\d+(，|,)?\\d*\\.?\\d*(毫米|厘米|分米|米|千米|公里|海里)$");
        nfToMf.put(new String[]{"重","排水量","重量"},"\\d+(，|,)?\\d*\\.?\\d*(克|千克|吨)$");
        nfToMf.put(new String[]{"吨位"},"\\d+-\\d+吨$");
        nfToMf.put(new String[]{"编制"},"\\d+(，|,)?\\d*人");
    }

    private void initNsMap(){
        nsMap = new HashMap<>();
        nsMap.put("http://kse.seu.edu.cn/rdb#","rdb");
        nsMap.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#","rdf");
        nsMap.put("http://www.w3.org/2000/01/rdf-schema#","rdfs");
        nsMap.put("http://www.w3.org/2002/07/owl#","owl");
        nsMap.put("http://www.w3.org/2001/XMLSchema#","xsd");
        nsMap.put("http://kse.seu.edu.cn/meta#","meta");
        nsMap.put("http://kse.seu.edu.cn/wgbq#","wgbq");
        nsMap.put("http://kse.seu.edu.cn/xgbg#","xgbg");
        nsMap.put("rdb","http://kse.seu.edu.cn/rdb#");
        nsMap.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        nsMap.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        nsMap.put("owl","http://www.w3.org/2002/07/owl#");
        nsMap.put("xsd","http://www.w3.org/2001/XMLSchema#");
        nsMap.put("meta","http://kse.seu.edu.cn/meta#");
        nsMap.put("wgbq","http://kse.seu.edu.cn/wgbq#");
        nsMap.put("xgbg","http://kse.seu.edu.cn/xgbg#");
    }

    public WashTool(OntParser ontParser){  //TODO:属性的清洗配置可以写入配置文件(利用JackSon解析)
        this.ontParser = ontParser;
        initNsMap();    //初始化命名空间与前缀的对应关系
        initNfToMf();   //初始化数据类型属性读取配置
    }


    /**
     * 数据清洗以实例为基本单位,按照数据类型属性的清洗配置来清洗,目前硬编码在代码中
     * @param ins
     */
    public void washingDpVals(Individual ins){

    }


    public void replaceDpToOpVals(OntModel ontModel,String ins1Pre,String dpPre,String opPre,String ins2Pre,String clsPre){
        String ins1Uri = getUriOf(ins1Pre);
        String ins2Uri = getUriOf(ins2Pre);
        String dpUri = getUriOf(dpPre);
        String opUri = getUriOf(opPre);
        String clsUri = getUriOf(clsPre);
        //尝试得到实例1
        Individual ins1 = ontModel.getIndividual(ins1Uri);
        if(ins1 == null) {  //实例1不存在,报错返回
            System.out.println("MissingIns:实例 " + ins1Pre + " 不存在");
            return;
        }
        //得到实例2对应的类(这个类一定存在)
        OntClass cls = ontModel.getOntClass(clsUri);
        //尝试得到实例2
        Individual ins2 = ontModel.getIndividual(ins2Uri);
        if(ins2 == null){  //知识库中不存在实例2
            ins2 = ontModel.createIndividual(ontModel.createOntResource(ins2Uri));  //创建实例2
            ins2.addProperty(RDF.type,cls);  //声明实例2的类(Jena API自动去除重复声明)
        }
        DatatypeProperty dp = ontModel.getDatatypeProperty(dpUri); //得到数据类型属性(可能为空)
        ObjectProperty op = ontModel.getObjectProperty(opUri);  //得到对象属性(可能为空)
        if(op == null){  //对象属性不存在
            op = ontModel.createObjectProperty(opUri);  //创建对象属性
        }
        replaceVal(ins1,dp,op,ins2,cls);
    }


    private void replaceVal(Individual ins1,DatatypeProperty dp,ObjectProperty op,Individual ins2,OntClass cls){
        if(dp != null){ //此数据类型属性存在
            //移除这个实例的这个数据类型属性值
        }


        //3.如果所有的实例都已经修改好了,移除这个数据类型属性
    }

    private String getUriOf(String pre){
        String[] tmp = pre.split(":");
        return nsMap.get(tmp[0]) + tmp[1];
    }

}
