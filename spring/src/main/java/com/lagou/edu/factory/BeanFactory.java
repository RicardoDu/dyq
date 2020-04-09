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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 应癫
 * <p>
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String, Object> map = new HashMap<>();  // 存储对象  一级缓存

        private static Map<String, Object> earlyMap = new HashMap<>();  // 存储对象  二级缓存

    private static Map<String, Object> basicMap = new HashMap<>();  // 存储对象  三级缓存

    private static Map<String, Object> currentMap = new HashMap<>();  // 存储对象  当前正在生成的缓存




    static {
        //initXML();
        try {
            initAnnoElementType("com.lagou.edu.service.impl",Service.class);
            initAnnoElementType("com.lagou.edu.utils", Component.class);
            initAnnoElementType("com.lagou.edu.dao.impl", Repository.class);
            //initAnnoElementFiled("com.lagou.edu", Autowired.class);
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

    private static void initAnnoElementFiled(String packageName, Class<? extends Annotation> ano) {
        List<Class<?>> classesAll = ClassUtil.getClasses(packageName);
        for (Class<?> classInfo : classesAll) {
            String className = ClassUtil.toLowerCaseFirstOne(classInfo.getName()); //将文件名转换
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
                map.put(id, o);

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
                Object parentObject = map.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if (method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject, map.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                map.put(parentId, parentObject);

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
     * 扫描范围为ElementType.TYPE且带value属性注解
     * @param packageName
     * @param ano
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    private static void initAnnoElementType(String packageName, Class<? extends Annotation> ano) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // 任务一： 扫描service(Dao)Impl文件夹 找到所有的@service（map集合）
        List<Class<?>> classesAll = ClassUtil.getClasses(packageName);
        for (Class<?> classInfo : classesAll) {
            String className = ClassUtil.toLowerCaseFirstOne(classInfo.getName()); //将文件名转换
            //2 判断类上面是否存在注入@service的注解 一个类最多存在一个@service注解
            Object serviceAnno = classInfo.getAnnotation(ano);
            if (serviceAnno != null) {
                Method[] methods = ano.getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if (method.getName().equalsIgnoreCase("value")) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        Object str = method.invoke(serviceAnno);
                        if(map.get(str)==null){
                            Object o = classInfo.getDeclaredConstructor().newInstance();// 实例化之后的对象
                            try {
                                map.put(str.toString(), o);
                            }catch (NullPointerException e){
                                System.out.println("注解调用错误"+ano.getName());
                                throw e;
                            }
                        }
                    }
                }
            }
        }
    }

    ////@Autowired注解 处理 (service 内可能引入多个dao)
    //                Field[] fields = classInfo.getDeclaredFields();
    //                for (Field field : fields) {
    //                    Annotation[] anns = field.getAnnotations();
    //                    for (Annotation anno : anns) {
    //                        if (anno instanceof Autowired) {
    //                            System.out.println("6666");
    //                            //转成Class
    //                            Class autoClass= (Class) field.getGenericType();
    //                            Field declaredField = classInfo.getDeclaredField(field.getName());
    //                            declaredField.setAccessible(true);
    //                            declaredField.set(map.get(par),map.get(ref));
    //                        }
    //                    }
    //                }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

}
