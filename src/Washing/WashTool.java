package Washing;

import Parser.OntParser;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        nfToMf.put(new String[]{"编制"},"\\d+(，|,)?\\d*人$");
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
        initNfToMf();   //初始化数据类型属性清洗配置
    }

    /**
     * 数据清洗以实例为基本单位,按照数据类型属性的清洗配置来清洗,目前硬编码在代码中
     * @param ins
     */
    public void washingDpVals(Individual ins){
        StmtIterator iterator = ins.listProperties();
        Statement st = null;
        while(iterator.hasNext()){
            st = iterator.nextStatement();
            Property prop = st.getPredicate();
            String originalVal = null;
            if(prop.hasProperty(RDF.type, OWL.DatatypeProperty)){  //当前属性是数据类型属性
                String val = st.getObject().asLiteral().toString().trim(); //得到属性的属性值
                originalVal = val;    //保留属性取值的原值
                if(val.matches("\\d+(，|,)?\\d*\\.?\\d*")){  //属性值为纯数字
                    val = val.replaceAll(",|，","");  //去掉纯数字里面的逗号
                }else{  //属性值带单位,则根据属性名判断可能带有哪些单位
                    String patternStr = findWashingConf(prop.getProperty(RDFS.label).getObject().asLiteral().toString().trim()); //获取清洗模式
                    if(patternStr == null) {  //目前不清晰该属性的属性值,因为没有合适的清洗模式
                        continue;
                    }else{  //需要清洗
                        if(val.matches(patternStr)){  //匹配清洗模式,可以清洗
                            val = val.replaceAll(",|，",""); //去掉逗号
                            val = val.replace("公里","千米").replace("小时","时");
                            val = val.replace("千米每小时","千米/时");
                        }else{  //不匹配清洗模式的数据(三元组)
                            System.out.println("WashingMissMatched : " + getPreOf(ins.getURI()) + "," + getPreOf(prop.getURI()) + "," + val);
                        }
                    }
                }
                if(!val.equals(originalVal)){  //输出清洗出变化的数据(四元组)
                    System.out.println("WashingMatched     : " + getPreOf(ins.getURI()) + "," + getPreOf(prop.getURI()) + "," + "ori:" + originalVal + "," + "now:" + val);
                }
            }
        }
    }

    /**
     * 根据数据类型属性的属性名,返回对应的清洗模式
     * @param propName
     * @return
     */
    private String findWashingConf(String propName){
        boolean matched = false;
        for(Map.Entry<String[],String> entry : nfToMf.entrySet()){
            String[] conditions = entry.getKey();
            for(String condition : conditions){
                matched = matched || (propName.endsWith(condition) ? true : false);
            }
            if(matched){
                return entry.getValue();
            }
        }
        return null;  //返回null代表这个属性的属性值无需清洗
    }

    /**
     * 将一个实例的某一个数据类型属性替换为特定的对象属性,并赋值
     * @param ontModel
     * @param ins1Pre &nbsp 实例1
     * @param dpPre &nbsp 实例1的原数据类型属性
     * @param opPre &nbsp 对象属性
     * @param ins2Pre &nbsp 对象属性的取值
     * @param clsPre &nbsp 对象属性的值所属的类别
     */
    public void replaceDpToOpVals(OntModel ontModel,String ins1Pre,String dpPre,String opPre,String ins2Pre,String clsPre){
        String ins1Uri = getUriOf(ins1Pre);
        String ins2Uri = getUriOf(ins2Pre);
        String dpUri = getUriOf(dpPre);
        String opUri = getUriOf(opPre);
        String clsUri = getUriOf(clsPre);
        //尝试得到实例1
        Individual ins1 = ontModel.getIndividual(ins1Uri);
        if(ins1 == null) {  //实例1不存在,报错返回
            System.out.println("MissingIns1        : 实例 " + ins1Pre + " 不存在");
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
        replaceVal(ontModel,ins1,dp,op,ins2,cls);  //实例1,实例2,实例2对应的类,对象属性都已存在
    }

    /**
     * 将实例1的一个数据类型属性的值替换为实例2
     * @param ontModel
     * @param ins1
     * @param dp
     * @param op
     * @param ins2
     * @param cls
     */
    private void replaceVal(OntModel ontModel,Individual ins1,DatatypeProperty dp,ObjectProperty op,Individual ins2,OntClass cls){
        if(dp != null){ //1.如果此数据类型属性存在,移除这个实例的这个数据类型属性
            StmtIterator iterator = ins1.listProperties(dp);  //列出这个属性的所有triple
            while(iterator.hasNext()){
                Statement statement = iterator.nextStatement();
                RDFNode tmp = statement.getObject();
                ins1.removeProperty(dp,tmp);  //删除实例1这个属性的值
            }
        }
        //2.在实例1和实例2之间加入对象属性关系(现在还不确定是否需要中间节点)
//        ins1.addProperty(op,ins2);  //不加中间节点的方式
        //加入中间节点的方式
        OntClass bkCls = ontModel.getOntClass(getUriOf("meta:blankNode"));
        Individual bkIns = ontModel.createIndividual(ontModel.createOntResource(nsMap.get("wgbq") + UUID.randomUUID().toString().replace("-","")));
        bkIns.addProperty(RDF.type,bkCls);
        ins1.addProperty(op,bkIns);
        ObjectProperty insIs = ontModel.getObjectProperty(nsMap.get("meta") + "实例");
        bkIns.addProperty(insIs,ins2);
        //TODO:如果所有的实例都已经修改好了,移除这个数据类型属性(在本方法中不做这件事情)
    }

    /**
     * 根据preLabel返回Uri
     * @param pre
     * @return
     */
    private String getUriOf(String pre){
        String[] tmp = pre.split(":");
        return nsMap.get(tmp[0]) + tmp[1];
    }
    /**
     * 根据uri返回preLabel
     * @param uri
     * @return
     */
    private String getPreOf(String uri){
        int d = uri.indexOf("#" + 1);
        String str1 = uri.substring(0,d);
        String str2 = uri.substring(d);
        return nsMap.get(str1) + str2;
    }

}
