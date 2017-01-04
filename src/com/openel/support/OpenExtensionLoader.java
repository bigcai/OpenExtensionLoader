package com.openel.support;

import com.openel.annotations.Activate;
import com.openel.interfaces.TestDemo;
import com.openel.utils.Holder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 扩展类加载器实现类：
 * 1.支持通过@Activate注解实现“加载开关”，可用于过滤器等扩展接口，实现多一个接口多个实现类
 * 2.缓存以及加载过的类，以便下次使用,缓存必须具备有可见性和线程安全；
 * 3.缓存以及创建过的类加载器；
 *
 * Created by caisz on 2017/1/3.
 */
public class OpenExtensionLoader<T> {

    /** 扩展类缓存 */
    private final Holder<Map<String, Map<String, Class<?>>>> cachedClasses = new Holder<Map<String,Map<String, Class<?>>>>();

    /** 扩展类加载器缓存 */
    private static final ConcurrentMap<Class<?>, OpenExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, OpenExtensionLoader<?>>();

    private final Class<?> type;

    private static final String CLASSPATH_EXTENSION_DIR = "META-INF/extension/";

    /**
     * 构造函数
     * @param type
     */
    private OpenExtensionLoader(Class<?> type) {
        // 同步构造方法
        synchronized ( OpenExtensionLoader.class ) {
            this.type = type;
        }
    }

    /**
     *  同步方法:获取扩展类缓存
     * @return
     */
    private Map<String,  Map<String, Class<?>>> getExtensionClasses() {
        Map<String, Map<String, Class<?>>> classes = cachedClasses.get();
        if (classes == null) {
            // 同步线程安全
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = this.loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }
    // 该方法已同步： 检测加载扩展类到缓存
    private Map<String, Map<String, Class<?>>> loadExtensionClasses() {
        Map<String, Map<String, Class<?>>> extensionClasses = new HashMap<String, Map<String, Class<?>>>();
        this.loadfile( extensionClasses, CLASSPATH_EXTENSION_DIR );
        return extensionClasses;
    }
    // 该方法已同步：从配置文件中加载扩展类
    private void loadfile(Map<String,  Map<String, Class<?>>> extensionClasses, String classpathExtensionDir) {
        Class<?> clazz = null;
        String name = null;
        String line = null;
        java.net.URL url = null;
        Enumeration<URL> urls = null;
        Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

        try {
            String fileName = classpathExtensionDir + type.getName();
            ClassLoader classLoader = OpenExtensionLoader.class.getClassLoader();

            // 获取扩展说明文件
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                if (urls.hasMoreElements()) {
                    url = urls.nextElement();
                }
            }
            // 解析扩展说明文件
            if( url !=  null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        int i = line.indexOf('=');
                        if (i > 0) {
                            // 解析获取扩展类型的名称，以及扩展实现类的路径
                            name = line.substring(0, i).trim();
                            line = line.substring(i + 1).trim();
                        }
                        if (line.length() > 0) {
                            // 获取扩展实现类
                            clazz = Class.forName(line, true, classLoader);
                            if(clazz != null ) {
                                // 如果不是激活则不加载，默认为激活
                                Boolean isActivate = clazz.getAnnotation( Activate.class ) == null? true: clazz.getAnnotation( Activate.class ).value();
                                if(  !isActivate ) {
                                    continue;
                                }
                                if (!type.isAssignableFrom(clazz)) {
                                    // 如果扩展实现类不是该接口的实现类就抛出异常
                                    throw new IllegalStateException("Error when load extension class(interface: " + type + ", class line: " + clazz.getName() + "), class " + clazz.getName() + "is not subtype of interface.");
                                }
                                else {
                                    classes.putIfAbsent(name, clazz);

                                }
                            }
                        }
                    }
                }
                extensionClasses.put( type.getName(), classes );
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取扩展加载器
     * @param type
     * @param <T>
     * @return
     */
    public static <T> OpenExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if(!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        OpenExtensionLoader<T> loader = (OpenExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new OpenExtensionLoader<T>(type));
            loader = (OpenExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    /**
     * 获取扩展加载器的扩展类Map
     */
    public Map<String, Class<?>> getClasses() {
        Map<String,  Map<String, Class<?>>> classes = this.getExtensionClasses();
        Map<String, Class<?>> classMap = classes.get( type.getName() );
        return classMap;
    }

    /**
     * 获取扩展类实例对象（不缓存该对象）
     * @return
     */
    public T getExtension() {
        Class<?> clazz = null;
        T obj = null;
        try {
            // 实例化对象
            Map<String,  Map<String, Class<?>>> classes = this.getExtensionClasses();
            Map<String, Class<?>> classMap = classes.get( type.getName() );
            if( classMap != null ) {
                for (String key : classMap.keySet()) {
                    clazz = classMap.get( key );
                    break;
                }
            }
            if( clazz == null ) {
                throw new IllegalStateException("Error when load extension class( interface [ " + type.getName() +" ] haven't extension of Class !)");
            }
            obj =  (T)clazz.newInstance();
            return obj;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final void main(String[] args) {
        // 获取加载器
        OpenExtensionLoader<TestDemo> openExtensionLoader = OpenExtensionLoader.getExtensionLoader( TestDemo.class );
        // 获取扩展实例
        TestDemo testDemo =  openExtensionLoader.getExtension();
        System.out.println( testDemo.name() );
        // 获取扩展类列表
        Map<String, Class<?>> classMap = openExtensionLoader.getClasses();
        Class<?> clazz = null;
        for (String key : classMap.keySet()) {
            clazz = classMap.get( key );
            System.out.println( clazz );
        }

    }

}
