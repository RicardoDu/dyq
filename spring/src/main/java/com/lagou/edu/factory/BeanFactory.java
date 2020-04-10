package com.lagou.edu.factory;

import com.lagou.edu.anno.Autowired;
import com.lagou.edu.anno.Component;
import com.lagou.edu.anno.Repository;
import com.lagou.edu.anno.Service;
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
     *缓存集合
     */
    private static Map<String, Object> map = new HashMap<>();  // 存储对象  一级缓存
    private static Map<String, Object> earlyMap = new HashMap<>();  // 存储对象  二级缓存
    private static Map<String, Object> basicMap = new HashMap<>();  // 存储对象  三级缓存
    private static Set<String> currentBean = new HashSet<>();  //是否为当前正创建
    private static Map<String,Set<String>> sonMap = new HashMap<>();  //autowired对应Bean
    private static List<Map<String,Object>> toDealList = new ArrayList<>(); //要装配的bean集合


    static {
        try {
            //将所有基础bean添加到三计缓存
            initAnnoElementType("com.lagou.edu.service.impl",Service.class);
            initAnnoElementType("com.lagou.edu.utils", Component.class);
            initAnnoElementType("com.lagou.edu.dao.impl", Repository.class);
            //
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private static void initXML() {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element = beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        // accountDao
                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象

                // 存储到map中待用
                basicMap.put(id, o);

            }

            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element = propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = basicMap.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if (method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject, basicMap.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                basicMap.put(parentId, parentObject);

            }


        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * ElementType.TYPE类注解(包含value属性值) 扫描并添加到三级缓存
     * @param packageName 路径
     * @param ano 注解类
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    private static void initAnnoElementType(String packageName, Class<? extends Annotation> ano) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // 任务一： 扫描service(Dao)Impl文件夹 找到所有的@service（map集合）
        List<Class<?>> classesAll = ClassUtil.getClasses(packageName);
        for (Class<?> classInfo : classesAll) {
            //2 判断类上面是否存在注入@service的注解 一个类最多存在一个@service注解
            Object serviceAnno = classInfo.getAnnotation(ano);
            Object o = classInfo.getDeclaredConstructor().newInstance();// 实例化之后的对象
            if (serviceAnno != null) {
                String beanName = getBeanNameWithAnnoValue(ano, serviceAnno, classInfo);
                basicMap.put(beanName, o);
                //@Autowired注解 处理
                Set<String> autoBean = collectAutoBean(classInfo);
                if(autoBean.size()>0){
                    sonMap.put(beanName,autoBean);
                }
            }
        }
    }

    /**
     * 将autoBean集中到Set中
     * @param classInfo
     * @return
     */
    private static Set<String> collectAutoBean(Class<?> classInfo) {
        Field[] fields = classInfo.getDeclaredFields();
        Set<String> autoBean = new HashSet<>();
        for (Field field : fields) {
            Annotation[] anns = field.getAnnotations();
            for (Annotation anno : anns) {
                //发现AutowiredBean加入autoBean Set集合 最后存放到sonMap
                if (anno instanceof Autowired) {
                    Class autoClass= (Class) field.getGenericType();
                    autoBean.add(autoClass.getName());
                }
            }
        }
        return autoBean;
    }

    /**
     * 存到三级缓存
     * @param ano
     * @param serviceAnno
     * @param classInfo
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static String getBeanNameWithAnnoValue(Class<? extends Annotation> ano, Object serviceAnno, Class<?> classInfo) throws IllegalAccessException, InvocationTargetException {
        String className = ClassUtil.toLowerCaseFirstOne(classInfo.getName()); //将文件名转换
        Method[] methods = ano.getMethods();
        for (int j = 0; j < methods.length; j++) {
            Method method = methods[j];
            if (method.getName().equalsIgnoreCase("value")) {
                Object str = method.invoke(serviceAnno);
                return String.valueOf(str);
            }
        }
        return className;
    }




    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return basicMap.get(id);
    }

}
