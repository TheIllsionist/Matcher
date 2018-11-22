package matchers.impl;

import matchers.SchemaMatcher;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import util.MatchType;
import util.VDTFUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 虚拟文档方法进行模式层匹配
 * 对于一对特定的本体对,要有一个特定的匹配器对象进行匹配
 */
public class VDSchemaMatcher implements SchemaMatcher {

    private OntModel o1 = null; //待匹配本体1
    private OntModel o2 = null; //待匹配本体2
    private int clsM = 0;  //本体1中类文档数
    private int clsN = 0;  //本体2中类文档数
    private int dpM = 0;   //本体1中dp文档数
    private int dpN = 0;   //本体2中dp文档数
    private int opM = 0;   //本体1中op文档数
    private int opN = 0;   //本体2中op文档数
    private Map<String,Map<String,Double>> o1TFMaps = null;  //缓存计算过的TF信息
    private Map<String,Map<String,Double>> o2TFMaps = null;  //缓存计算过的TF信息
    private Map<String,Integer> clsBsVec = null;    //类匹配时的向量空间坐标基矢量(唯一单词,第几维)
    private Map<String,Integer> dpBsVec = null;    //DP匹配时的向量空间坐标基矢量(唯一单词,第几维)
    private Map<String,Integer> opBsVec = null;    //OP匹配时的向量空间坐标基矢量(唯一单词,第几维)
    private Map<String,Integer> clsIdfInfo = null;  //类匹配时坐标基矢量中每个词的逆文档频率
    private Map<String,Integer> dpIdfInfo = null;   //DP匹配时坐标基矢量中每个词的逆文档频率
    private Map<String,Integer> opIdfInfo = null;   //OP匹配时坐标基矢量中每个词的逆文档频率

    private VDTFUtil vdtfUtil = null;   //计算虚拟文档TF信息的工具(为了可复用和可扩展性,将提取TF信息的过程独立出一个类)
    private Map<String,Double> localConf = null;    //提取本地信息的配置
    private Map<String,Double> clsNeiConf = null;   //类匹配时周边信息配置
    private Map<String,Double> propNeiConf = null;  //属性匹配时周边信息配置


    public VDSchemaMatcher(OntModel o1,OntModel o2,VDTFUtil vdtfUtil,Map<String,Double> localConf,Map<String,Double> clsNeiConf,Map<String,Double> propNeiConf){
        this.o1 = o1;
        this.o2 = o2;
        this.vdtfUtil = vdtfUtil;
        this.localConf = localConf;
        this.clsNeiConf = clsNeiConf;
        this.propNeiConf = propNeiConf;
        o1TFMaps = new HashMap<>();
        o2TFMaps = new HashMap<>();
        clsBsVec = new HashMap<>();
        dpBsVec = new HashMap<>();
        opBsVec = new HashMap<>();
        clsIdfInfo = new HashMap<>();
        dpIdfInfo = new HashMap<>();
        opIdfInfo = new HashMap<>();
    }

    public void setVDTFUtil(VDTFUtil vdtfUtil){
        this.vdtfUtil = vdtfUtil;
    }

    public void setLocalConf(Map<String,Double> localConf){
        this.localConf = localConf;
    }

    public void setClsNeiConf(Map<String,Double> clsNeiConf){
        this.clsNeiConf = clsNeiConf;
    }

    public void setPropNeiConf(Map<String,Double> propNeiConf){
        this.propNeiConf = propNeiConf;
    }

    /**
     * 计算两个本体中两个类的相似度
     * @param cls1
     * @param cls2
     * @return
     * @throws Exception
     */
    @Override
    public double simOfClass(OntClass cls1, OntClass cls2) throws Exception{
        if(clsBsVec.size() == 0){  //类匹配坐标基矢量未初始化
            System.out.println("init class match context....");
            initClsMathchInfo();  //初始化类匹配坐标基矢量,类匹配IDF信息,同时缓存中间计算结果
            System.out.println("init success!");
        }else{
            System.out.println("class match context has inited.");
        }
        Map<String,Double> VDTFMap1 = o1TFMaps.get(cls1.getURI());  //文档1
        Map<String,Double> VDTFMap2 = o2TFMaps.get(cls2.getURI());  //文档2
        double[] vec1 = convertVD2Vec(VDTFMap1,(clsM + clsN),clsIdfInfo,clsBsVec);  //将文档1转换为向量,转换过程中计算 tf * idf
        double[] vec2 = convertVD2Vec(VDTFMap2,(clsM + clsN),clsIdfInfo,clsBsVec);  //将文档2转换为向量,转换过程中计算 tf * idf
        return simOf2Vec(vec1,vec2);
    }

    /**
     * 计算两个本体中两个数据类型属性的相似度
     * @param pro1
     * @param pro2
     * @return
     * @throws Exception
     */
    @Override
    public double simOfDp(DatatypeProperty pro1, DatatypeProperty pro2) throws Exception{
        if(dpBsVec.size() == 0){  //dp匹配坐标基矢量未初始化
            System.out.println("init dp match context....");
            initDpMatchInfo();
            System.out.println("init success!");
        }else{
            System.out.println("dp match context has inited.");
        }
        Map<String,Double> VDTFMap1 = o1TFMaps.get(pro1.getURI());
        Map<String,Double> VDTFMap2 = o2TFMaps.get(pro2.getURI());
        double[] vec1 = convertVD2Vec(VDTFMap1,(dpM + dpN),dpIdfInfo,dpBsVec);
        double[] vec2 = convertVD2Vec(VDTFMap2,(dpM + dpN),dpIdfInfo,dpBsVec);
        return simOf2Vec(vec1,vec2);
    }

    /**
     * 计算两个本体中两个对象属性的相似度
     * @param pro1
     * @param pro2
     * @return
     * @throws Exception
     */
    @Override
    public double simOfOp(ObjectProperty pro1, ObjectProperty pro2) throws Exception{
        if(opBsVec.size() == 0){
            System.out.println("init op match context....");
            initOpMatchInfo();
            System.out.println("init success!");
        }else{
            System.out.println("op match context has inited.");
        }
        Map<String,Double> VDTFMap1 = o1TFMaps.get(pro1.getURI());
        Map<String,Double> VDTFMap2 = o2TFMaps.get(pro2.getURI());
        double[] vec1 = convertVD2Vec(VDTFMap1,(opM + opN),opIdfInfo,opBsVec);
        double[] vec2 = convertVD2Vec(VDTFMap2,(opM + opN),opIdfInfo,opBsVec);
        return simOf2Vec(vec1,vec2);
    }

    /**
     * 计算两个向量之间的余弦相似度
     * @param vec1
     * @param vec2
     * @return
     */
    private double simOf2Vec(double[] vec1,double[] vec2){
        if(vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0 || vec1.length != vec2.length)
            return 0.0;
        double target = 0.0;   //最终结果
        double fenzi = 0.0;    //计算分子
        double sumVec1 = 0.0;  //分母的一部分
        double sumVec2 = 0.0;  //分母的另一部分
        for(int i = 0;i < vec1.length;i++){
            if(vec1[i] == 0 || vec2[i] == 0){
                if (vec1[i] != 0) {
                    sumVec1 += vec1[i] * vec1[i];
                }
                if (vec2[i] != 0) {
                    sumVec2 += vec2[i] * vec2[i];
                }
            }else{
                fenzi += vec1[i] * vec2[i];
                sumVec1 += vec1[i] * vec1[i];
                sumVec2 += vec2[i] * vec2[i];
            }
        }
        if(sumVec1 == 0 || sumVec2 == 0)
            target = 0.0;
        else
            target = (fenzi / (Math.sqrt(sumVec1) * Math.sqrt(sumVec2)));
        return Double.valueOf(String.format("%.3f",target));  //取小数点后3位
    }

    /**
     * 给定全局IDF信息和
     * @param VDTFMap
     * @param docNum
     * @param idfInfo
     * @param bsVec
     * @return
     */
    private double[] convertVD2Vec(Map<String,Double> VDTFMap, int docNum, Map<String,Integer> idfInfo, Map<String,Integer> bsVec){
        double[] vec = new double[bsVec.size()];  //向量维数和基矢量维数相等
        int index = 0;
        for(String token : VDTFMap.keySet()){
            double TF = VDTFMap.get(token);     //该token的tf
            double IDF = Math.log((double)docNum / (idfInfo.get(token) + 1)); //该token的idf
            index = bsVec.get(token);  //得到该token对应的维度
            vec[index] = TF * IDF;
        }
        return vec;
    }

    /**
     * 初始化进行类匹配所需要的所有信息,包括:
     * 1.类匹配时的向量空间坐标基矢量
     * 2.类匹配时坐标基矢量中每个词的逆文档频率
     * 3.在计算的过程中,会缓存为每个实体计算出来的TF信息以避免重复计算
     */
    private void initClsMathchInfo() throws Exception{
        Set<String> clsBsSet = new HashSet<>();
        ExtendedIterator<OntClass> o1Clses = o1.listClasses();
        while(o1Clses.hasNext()){
            OntClass cls = o1Clses.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o1,cls.getURI(),MatchType.CLASS_MATCH,localConf,clsNeiConf);//计算实体虚拟文档的TF信息
            o1TFMaps.put(cls.getURI(),map);  //缓存当前类的TF计算结果
            clsBsSet.addAll(map.keySet());   //加入所有唯一单词
            clsM++;
            for(String unique : map.keySet()){   //更新IDF信息
                if(clsIdfInfo.containsKey(unique)){
                    clsIdfInfo.put(unique,clsIdfInfo.get(unique) + 1);
                }else{
                    clsIdfInfo.put(unique,1);
                }
            }
        }
        ExtendedIterator<OntClass> o2Clses = o2.listClasses();
        while(o2Clses.hasNext()){
            OntClass cls = o2Clses.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o2,cls.getURI(),MatchType.CLASS_MATCH,localConf,clsNeiConf);//计算实体虚拟文档的TF信息
            o2TFMaps.put(cls.getURI(),map); //缓存当前类的TF计算结果
            clsBsSet.addAll(map.keySet());  //加入所有唯一单词
            clsN++;
            for(String unique : map.keySet()){
                if(clsIdfInfo.containsKey(unique)){
                    clsIdfInfo.put(unique,clsIdfInfo.get(unique) + 1);
                }else{
                    clsIdfInfo.put(unique,1);
                }
            }
        }
        clsBsVec = setToVec(clsBsSet);  //将唯一单词集合转换为类匹配向量空间的坐标基矢量
    }

    /**
     * 初始化进行数据类型匹配所需要的所有信息,包括:
     * 1.数据类型属性匹配时的向量空间坐标基矢量
     * 2.数据类型属性匹配时坐标基矢量中每个词的逆文档频率
     * 3.在计算的过程中,会缓存为每个实体计算出来的TF信息以避免重复计算
     */
    private void initDpMatchInfo() throws Exception{
        Set<String> dpBsSet = new HashSet<>();
        ExtendedIterator<DatatypeProperty> o1Dps = o1.listDatatypeProperties();
        while(o1Dps.hasNext()){
            DatatypeProperty dp = o1Dps.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o1,dp.getURI(),MatchType.DP_MATCH,localConf,propNeiConf);
            o1TFMaps.put(dp.getURI(),map);  //缓存当前属性的TF计算结果
            dpBsSet.addAll(map.keySet());  //加入所有唯一单词
            dpM++;
            for(String unique : map.keySet()){
                if(dpIdfInfo.containsKey(unique)){
                    dpIdfInfo.put(unique,dpIdfInfo.get(unique) + 1);
                }else{
                    dpIdfInfo.put(unique,1);
                }
            }
        }
        ExtendedIterator<DatatypeProperty> o2Dps = o2.listDatatypeProperties();
        while(o2Dps.hasNext()){
            DatatypeProperty dp = o2Dps.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o2,dp.getURI(),MatchType.DP_MATCH,localConf,propNeiConf);
            o2TFMaps.put(dp.getURI(),map);  //缓存当前属性的TF计算结果
            dpBsSet.addAll(map.keySet());   //加入所有唯一单词
            dpN++;
            for(String unique : map.keySet()){
                if(dpIdfInfo.containsKey(unique)){
                    dpIdfInfo.put(unique,dpIdfInfo.get(unique) + 1);
                }else{
                    dpIdfInfo.put(unique,1);
                }
            }
        }
        dpBsVec = setToVec(dpBsSet);
    }

    /**
     * 初始化进行对象属性匹配所需要的所有信息,包括:
     * 1.对象属性匹配时的向量空间坐标基矢量
     * 2.对象属性匹配时坐标基矢量中每个词的逆文档频率
     * 3.在计算的过程中,会缓存为每个实体计算出来的TF信息以避免重复计算
     */
    private void initOpMatchInfo() throws Exception{
        Set<String> opBsSet = new HashSet<>();
        ExtendedIterator<ObjectProperty> o1Ops = o1.listObjectProperties();
        while(o1Ops.hasNext()){
            ObjectProperty op = o1Ops.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o1,op.getURI(),MatchType.OP_MATCH,localConf,propNeiConf);
            o1TFMaps.put(op.getURI(),map);  //缓存当前属性的TF计算结果
            opBsSet.addAll(map.keySet());  //加入所有唯一单词
            opM++;
            for(String unique : map.keySet()){
                if(opIdfInfo.containsKey(unique)){
                    opIdfInfo.put(unique,opIdfInfo.get(unique) + 1);
                }else{
                    opIdfInfo.put(unique,1);
                }
            }
        }
        ExtendedIterator<ObjectProperty> o2Ops = o2.listObjectProperties();
        while(o2Ops.hasNext()){
            ObjectProperty op = o2Ops.next();
            Map<String,Double> map = vdtfUtil.VDTFMapOf(o2,op.getURI(),MatchType.OP_MATCH,localConf,propNeiConf);
            o2TFMaps.put(op.getURI(),map);  //缓存当前属性的TF计算结果
            opBsSet.addAll(map.keySet());  //加入所有唯一单词
            opN++;
            for(String unique : map.keySet()){
                if(opIdfInfo.containsKey(unique)){
                    opIdfInfo.put(unique,opIdfInfo.get(unique) + 1);
                }else{
                    opIdfInfo.put(unique,1);
                }
            }
        }
        opBsVec = setToVec(opBsSet);
    }

    /**
     * 将唯一单词集合转为一个向量空间的坐标基矢量
     * @param set
     * @return
     */
    private Map<String,Integer> setToVec(Set<String> set){
        Map<String,Integer> map = new HashMap<>();
        int index = 0;
        for(String unique : set){
            map.put(unique,index++);
        }
        return map;
    }

}
