package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.anno.Autowired;
import com.lagou.edu.anno.Component;
import com.lagou.edu.anno.Transactional;
import com.lagou.edu.utils.ClassUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author 应癫
 * <p>
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 代理工厂类bean
     */
    public static final String PROXY_FACTORY = "proxyFactory";

    private static ProxyFactory proxyFactory;
    /**
     * 缓存集合
     * beanMap 一级缓存
     * earlyMap 二级缓存
     * basicMap 三级缓存
     * currentBean 当前正创建bean集合
     * sonMap beanName及其依赖beanName集合
     */
    private static Map<String, Object> beanMap = new HashMap<>();
    private static Map<String, Object> earlyMap = new HashMap<>();
    private static Map<String, Object> basicMap = new HashMap<>();
    private static Set<String> currentBean = new HashSet<>();
    private static Set<String> typeBeanName = new HashSet<>();
    private static Map<String, Set<String>> sonMap = new HashMap<>();
    private static Set<String> transferBean = new HashSet<>();


    static {
        try {
            //首先初始化 组件类bean 此类组件直接存在一级缓存内
            initAnnoElementComponent(BasicBeanMatchEnum.Component.getName(), BasicBeanMatchEnum.Component.getValue());
            //代理工厂及事务类文件初始化
            initAnnoElementTransfer(BasicBeanMatchEnum.Transfer.getName(), BasicBeanMatchEnum.Transfer.getValue());
            //初始化 Service类bean Repository为Dao层注解 此层内部Autowired 允许调用Component 及其他DaoImpl
            initAnnoElementService(BasicBeanMatchEnum.Repository.getName(), BasicBeanMatchEnum.Repository.getValue());
            //初始化 Service类bean Service为Service层注解 此层内部Autowired 可调用所有Bean
            initAnnoElementService(BasicBeanMatchEnum.Service.getName(), BasicBeanMatchEnum.Service.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 事务类工具初始化
     * @param name
     * @param value
     */
    private static void initAnnoElementTransfer(String name, Class<? extends Annotation> value) throws Exception {
        //事务类 处理
        initAnnoElementService(name,value);
        //处理代理工厂
        initBeanWithAutoWired(value,ProxyFactory.class);
        beanAssemblyCycle(PROXY_FACTORY, sonMap.get(PROXY_FACTORY));
        //初始化代理工厂
        proxyFactory = (ProxyFactory) beanMap.get(PROXY_FACTORY);
        sonMap.remove(PROXY_FACTORY);

    }

    /**
     * 服务类bean处理
     * @param name
     * @param value
     * @throws Exception
     */
    private static void initAnnoElementService(String name, Class<? extends Annotation> value) throws Exception {
        //初始化Service层全部bean
        initAnnoElementType(name,value);
        //处理其权限内的Autoired
        initAnnoElementFiled();
        //清空权限关系
        sonMap.clear();
    }

    private static void initAnnoElementComponent(String packageName, Class<? extends Annotation> ano) throws Exception {
        // 任务一： 扫描service(Dao)Impl文件夹 找到所有的@service（map集合）
        List<Class<?>> classesAll = ClassUtil.getClasses(packageName);
        for (Class<?> classInfo : classesAll) {
            //2 判断类上面是否存在注入@service的注解 一个类最多存在一个@service注解
            Object serviceAnno = classInfo.getAnnotation(ano);
            // 实例化之后的对象
            if (serviceAnno != null) {
                putBeanToMap(classInfo, getBeanNameWithAnnoValue(serviceAnno, classInfo),beanMap);
            }
        }
    }

    /**
     * 根据classInfo 生成bean 放入到对应的map中
     * @param classInfo
     * @param beanName
     * @param setToMap
     * @return
     * @throws Exception
     */
    private static Object putBeanToMap(Class<?> classInfo, String beanName,Map setToMap) throws Exception {
        Object o;
        try{
            o = classInfo.getDeclaredConstructor().newInstance();
            //默认value将文件名转换
            //String beanName = ;
            if(!typeBeanName.add(beanName)){
                //注解名重复
                Object aClass = (beanMap.get(beanName)==null?(earlyMap.get(beanName)==null?basicMap.get(beanName):earlyMap.get(beanName)):beanMap.get(beanName));
                System.out.println(classInfo.getName()+"注解名与"+aClass.getClass().getName()+"重复");
                throw new Exception();
            }
            if(setToMap!=null){
                setToMap.put(beanName, o);
            }
        }catch (Exception e){
            System.out.println(classInfo.getName()+"无需创建bean");
            throw e;
        }
        return o;
    }

    /**
     * 遍历sonMap处理autowired
     */
    private static void initAnnoElementFiled() throws Exception {
        for (Map.Entry<String, Set<String>> entry : sonMap.entrySet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            beanAssemblyCycle(key, value);
        }
    }

    /**
     * 循环装配
     *
     * @param k beanName
     * @param v 自动的集合
     * @return
     */
    private static Object beanAssemblyCycle(String k, Set<String> v) throws Exception {
        //map中存在bean？存在说明已经对其进行过装配 直接返回;
        Object keyBean = beanMap.get(k);
        if (keyBean != null) {
            return keyBean;
        }
        //currentBean中存在？
        if (currentBean.add(k)) {
            List<Map<String, Object>> toDealList = new ArrayList<>();
            //遍历Set<String> String--存储全路径 （再处理为beanName）
            for (String name : v) {
                //此时name为fileName+"#"+ 存储的bean的name 或头部无ElementType注解的类的全路径
                String fileName = name.split("#")[0];
                String beanName = name.split("#")[1];
                Object autoBean = doRealBean(beanName);
                Map<String, Object> autoMap = new HashMap<>();
                autoMap.put(fileName, autoBean);
                //加入toDealList
                toDealList.add(autoMap);
            }
            if (earlyMap.get(k)!=null) {
                //earlyMap获取bean  通过toDealList装配 -> beanMap
                keyBean = assemblyBean(earlyMap.get(k), toDealList);
                putEarlyToBeanMap(k,keyBean);
            } else {
                //basicMap获取bean  通过toDealList装配 -> beanMap(是否进一步操作？)
                keyBean = assemblyBean(basicMap.get(k), toDealList);
                putBasicToBeanMap(k,keyBean);
            }
            currentBean.remove(k);
        } else {
            keyBean = getCycleBean(k);
        }
        return keyBean;
    }

    /**
     * 完整bean
     * @param fullName
     * @return
     * @throws Exception
     */
    private static Object doRealBean(String fullName) throws Exception {
        String[] fullNames = fullName.split("\\.");
        String beanName = ClassUtil.toLowerCaseFirstOne(fullNames[fullNames.length-1]);
        Object autoBean = beanMap.get(beanName);//t为基础bean
        //判断map中是否存在该bean
        if (autoBean == null) {
            //不存在 判断currentBean
            if (currentBean.contains(beanName)) {
                //是:出现循环依赖
                autoBean = getCycleBean(beanName);
            } else {
                autoBean = basicMap.get(beanName);
                if(autoBean == null){
                    //缓存中不存在此bean 根据全路径 实例化
                    putBeanToMap(Class.forName(fullName),fullName,null);
                    System.out.println(fullName+"实例化");
                }else{
                    //否 继续循环装配autoBean
                    autoBean = beanAssemblyCycle(beanName, sonMap.get(beanName));
                }
            }
        }
        return autoBean;
    }

    /**
     * 装配BEAN
     * @param keyBean
     * @param toDealList
     */
    private static Object assemblyBean(Object keyBean, List<Map<String, Object>> toDealList) throws NoSuchFieldException, IllegalAccessException {
        for (Map<String, Object> t : toDealList) {
            for (Map.Entry<String, Object> entry : t.entrySet()) {
                Object value = entry.getValue();
                Field field = keyBean.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(keyBean,value);
            }
        }
        return keyBean;
    }

    /**
     * 出现循环依赖处理
     * @param k
     * @return
     */
    private static Object getCycleBean(String k) {
        //是:出现循环依赖
        Object autoBean = earlyMap.get(k);
        if(autoBean == null){
            autoBean =  putBasicToEarly(k,null);
        }
        return autoBean;
    }

    /**
     * 把bean从二级缓存取出并放到一级中
     * @param k beanName
     */
    private static Object putEarlyToBeanMap(String k,Object bean) {
        bean = bean==null?earlyMap.get(k):bean;
        beanMap.put(k,bean);
        earlyMap.remove(k);
        return bean;
    }

    /**
     * 将keyBean从三级缓存中升至二级缓存
     * @param k
     */
    private static Object putBasicToEarly(String k,Object bean) {
        bean = bean==null?basicMap.get(k):bean;
        if(transferBean.contains(k)){
            bean = proxyFactory.getJdkProxy(bean);
        }
        earlyMap.put(k, bean);
        basicMap.remove(k);
        return bean;
    }

    /**
     * 把bean从三级缓存取出并放到一级中
     * @param k beanName
     */
    private static Object putBasicToBeanMap(String k,Object bean) {
        bean = bean==null?basicMap.get(k):bean;
        if(transferBean.contains(k)){
            bean = proxyFactory.getJdkProxy(bean);
        }
        beanMap.put(k,bean);
        basicMap.remove(k);
        return bean;
    }

    /**
     * ElementType.TYPE类注解(包含value属性值) 扫描并添加到三级缓存
     *
     * @param packageName 路径
     * @param ano         注解类
     */
    private static void initAnnoElementType(String packageName, Class<? extends Annotation> ano) throws Exception {
        // 任务一： 扫描service(Dao)Impl文件夹 找到所有的@service（map集合）
        List<Class<?>> classesAll = ClassUtil.getClasses(packageName);
        for (Class<?> classInfo : classesAll) {
            initBeanWithAutoWired(ano, classInfo);
        }
    }

    private static void initBeanWithAutoWired(Class<? extends Annotation> ano, Class<?> classInfo) throws Exception {
        //2 判断类上面是否存在注入@service的注解 一个类最多存在一个@service注解
        Object serviceAnno = classInfo.getAnnotation(ano);
        // 实例化之后的对象
        if (serviceAnno != null) {
            String beanName = getBeanNameWithAnnoValue(serviceAnno, classInfo);
            putBeanToMap(classInfo,beanName,basicMap);
            if(classInfo.getAnnotation(Transactional.class)!=null){
                transferBean.add(beanName);
            }
            //@Autowired注解 处理
            Set<String> autoBean = collectAutoBean(classInfo);
            if (autoBean.size() > 0) {
                sonMap.put(beanName, autoBean);
            } else {
                //把bean从三级缓存取出并放到map中
                putBasicToBeanMap(beanName,null);
            }
        }
    }


    /**
     * 将autoBean集中到Set中
     * @param classInfo 类文件
     */
    private static Set<String> collectAutoBean(Class<?> classInfo) throws InvocationTargetException, IllegalAccessException {
        Field[] fields = classInfo.getDeclaredFields();
        Set<String> autoBean = new HashSet<>();
        for (Field field : fields) {
            Annotation[] annos = field.getAnnotations();
            for (Annotation anno : annos) {
                //发现AutowiredBean加入autoBean Set集合 最后存放到sonMap
                if (anno instanceof Autowired) {
                    Class autoClass = (Class) field.getGenericType();
                    //定位到class
                    Annotation[] annotations = autoClass.getAnnotations();
                    //默认全路径名
                    String autoName = autoClass.getName();
                    if (annotations.length > 0) {
                        for (Annotation annotation : annotations) {
                            //获取注解value
                            String annoName = getBeanNameWithAnnoValue(annotation, autoClass);
                            //存在value 设置beanName
                            if (!autoName.equals(annoName)) {
                                autoName = annoName;
                                break;
                            }
                        }
                    }
                    autoBean.add(field.getName()+"#"+autoName);
                    break;
                }
            }
        }
        return autoBean;
    }

    /**
     * 获取注解value
     * @param serviceAnno
     */
    private static String getBeanNameWithAnnoValue(Object serviceAnno, Class<?> classInfo)
            throws IllegalAccessException, InvocationTargetException {
        Method[] methods = serviceAnno.getClass().getMethods();
        for (Method method : methods) {
            if ("value".equalsIgnoreCase(method.getName())) {
                Object str = method.invoke(serviceAnno);
                if(str!=null&&!"".equals(str.toString())){
                    return str.toString();
                }else{
                    //路径名-包名
                    return ClassUtil.toLowerCaseFirstOne(classInfo.getName().replace(classInfo.getPackageName(),"").substring(1));
                }
            }
        }
        return classInfo.getName();
    }


    /**
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */
    public static Object getBean(String id) {
        return beanMap.get(id);
    }

}
