package util;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import tokenizer.Tokenizer;
import java.util.*;

/**
 * Created by The Illsionist on 2018/11/19.
 * 虚拟文档方法中提取实体虚拟文档中TF信息的工具
 */
public class VDTFUtil {

    private Tokenizer tokenizer = null;  //分词去停用词工具

    public VDTFUtil(Tokenizer tokenizer){
        this.tokenizer = tokenizer;
    }

    /**
     * 可以通过调用setter方法来替换分词去停用词的工具,以尝试新的分词方法
     * @param tokenizer
     */
    public void setTokenizer(Tokenizer tokenizer){
        this.tokenizer = tokenizer;
    }

    public Tokenizer getTokenizer(){
        return tokenizer;
    }

    /**
     * 获取某个本体资源的虚拟文档TF信息,根据信息获取的配置,可能包括该资源的本地信息和周边信息
     * @param model &nbsp 本体
     * @param uri &nbsp 指定资源的URI
     * @param type &nbsp 资源类型:类,DP,OP,INS
     * @param localConf &nbsp 本地信息的权重配置
     * @param neiborConf &nbsp 周边信息的权重配置
     * @return
     * TODO:以后可能会添加新的代码
     */
    public Map<String,Double> VDTFMapOf(OntModel model,String uri,MatchType type,Map<String,Double> localConf,Map<String,Double> neiborConf) throws Exception{
        OntResource resource = model.getOntResource(uri);
        if(resource == null)
            throw new NullPointerException();
        Map<String,Double> localTFMap = localTFMapOf(resource,localConf);  //得到该资源的本地虚拟文档TF信息
        //下面都是对邻居信息的处理
        OntClass cls = null;
        OntProperty ontProp = null;
        switch(type){
            case CLASS_MATCH:{  //匹配任务是匹配类
                cls = resource.asClass();
                for(Map.Entry<String,Double> entry : neiborConf.entrySet()){
                    switch (entry.getKey()){
                        case "supClass":{    //类的父类作为邻居信息
                            Iterator<OntClass> supClses = cls.listSuperClasses();
                            while(supClses.hasNext()){
                                OntClass tSup = supClses.next();
                                if(tSup.isAnon() || !tSup.isClass())
                                    continue;
                                Map<String,Double> neiMap = localTFMapOf(tSup,localConf);
                                mergeLocMapWithNeiMap(localTFMap,neiMap,entry.getValue());
                            }
                        }break;
                        case "domainOf":{  //类所"拥有"的属性作为邻居信息
                            List<OntProperty> props = propsOfCls(model,cls);
                            for (OntProperty prop : props) {
                                if(prop.isAnon() || (!prop.isDatatypeProperty() && !prop.isObjectProperty())){
                                    continue;
                                }
                                Map<String,Double> neiMap = localTFMapOf(prop,localConf);
                                mergeLocMapWithNeiMap(localTFMap,neiMap,entry.getValue());
                            }
                        }break;
                    }
                }
            }break;
            case DP_MATCH:  //匹配任务是匹配数据类型属性或者对象属性
            case OP_MATCH:{
                ontProp = resource.asProperty();
                for(Map.Entry<String,Double> entry : neiborConf.entrySet()){
                    switch (entry.getKey()){
                        case "supProp":{   //属性的父属性作为邻居信息
                            ExtendedIterator< ? extends OntProperty> supProps = ontProp.listSuperProperties(true);
                            while(supProps.hasNext()){
                                OntProperty prop = supProps.next();
                                if(prop.isAnon() || (!prop.isDatatypeProperty() && !prop.isObjectProperty())){
                                    continue;
                                }
                                Map<String,Double> neiMap = localTFMapOf(prop,localConf);
                                mergeLocMapWithNeiMap(localTFMap,neiMap,entry.getValue());
                            }
                        }break;
                        case "domains":{  //属性的domain作为邻居信息
                            ExtendedIterator<? extends OntResource> domains = ontProp.listDomain();
                            while(domains.hasNext()){
                                OntResource tDom = domains.next();
                                if(tDom.isAnon() || !tDom.isClass()){
                                    continue;
                                }
                                Map<String,Double> neiMap = localTFMapOf(tDom,localConf);
                                mergeLocMapWithNeiMap(localTFMap,neiMap,entry.getValue());
                            }
                        }break;
                    }
                }
            }break;
        }
        //经过以上步骤,本地信息和邻居信息都已被加入到localTFMap中
        normalization(localTFMap);  //计算词频
        return localTFMap;
    }

    /**
     * 返回某个类所"拥有"的属性,这里指的是将定义域指定为该类的属性
     * @param model
     * @param cls
     * @return
     */
    private List<OntProperty> propsOfCls(OntModel model,OntClass cls){
        List<OntProperty> res = new ArrayList<>();
        Iterator<OntProperty> props = model.listOntProperties();  //迭代模型中的所有属性,寻找将OntClass作为domain的属性
        while(props.hasNext()){
            OntProperty tProp = props.next();
            if(tProp.hasProperty(RDFS.domain,cls)){
                res.add(tProp);
            }else{
                continue;
            }
        }
        return res;
    }


    /**
     * 将周边TF信息合并到本体TF信息中
     * @param locMap
     * @param neiMap
     * @param neiWeight
     */
    private void mergeLocMapWithNeiMap(Map<String,Double> locMap,Map<String,Double> neiMap,double neiWeight){
        for(String key : neiMap.keySet()){
            double neiVal = neiMap.get(key);
            if(locMap.containsKey(key)){    //这个token在本地信息中已存在
                locMap.put(key,locMap.get(key) + neiWeight * neiVal);
            }else{
                locMap.put(key, 0.0 + neiWeight * neiVal);
            }
        }
    }

    private void normalization(Map<String,Double> map){
        double total = 0;
        for(String key : map.keySet()){
            total += map.get(key);
        }
        for(String key : map.keySet()){
            map.put(key,map.get(key) / total);  //计算出真正的词频(从这里可以看到,虚拟文档改写了TF)
        }
    }

    /**
     * 获取某个本体资源的本地TF信息
     * @param resource
     * @param localConf &nbsp 提取哪些本地信息以及信息的权重配置
     * @return
     * @throws Exception
     * TODO:此方法可能会增加新的代码
     */
    public Map<String,Double> localTFMapOf(OntResource resource,Map<String,Double> localConf) throws Exception{
        if(resource == null){
            throw new NullPointerException();
        }
        Map<String,Double> tfMap = new HashMap<>();  //最终要返回的本地TF信息
        //根据配置提取信息
        for (Map.Entry<String,Double> entry : localConf.entrySet()) {
            switch(entry.getKey()){
                case "label":{
                    Iterator<RDFNode> labels = resource.listLabels(null);  //列出该实体的所有可读名称
                    while(labels.hasNext()){
                        String label = labels.next().toString();
                        List<String> tokens = tokenizer.tokensOfStr(label);  //tokens是允许有重复的
                        addTokensToMap(tokens,tfMap,entry.getValue());
                    }
                }break;
                case "comment":{
                    Iterator<RDFNode> comments = resource.listComments(null);  //列出该实体的所有释义描述
                    while(comments.hasNext()){
                        String comment = comments.next().toString();
                        List<String> tokens = tokenizer.tokensOfStr(comment);  //tokens是允许有重复的
                        addTokensToMap(tokens,tfMap,entry.getValue());
                    }
                }break;
            }
        }
        return tfMap;
    }


    /**
     * 细节操作,将一系列tokens按照指定权重weight加入一个tokenMap中
     * @param tokens
     * @param tokenMap
     * @param weight
     */
    private void addTokensToMap(List<String> tokens,Map<String,Double> tokenMap,double weight){
        for (String token : tokens) {
            if(tokenMap.containsKey(token)){
                tokenMap.put(token,tokenMap.get(token) + 1 * weight);
            }else{
                tokenMap.put(token,1 * weight);
            }
        }
    }


}
