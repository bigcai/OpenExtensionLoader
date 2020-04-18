# OpenExtensionLoader
这是一个简约的线程安全的扩展类加载器，类似于JDK的SPI,你可以用来学习，或者修改。





 ### 扩展类加载器实现类：
 * 1.支持通过@Activate注解实现“加载开关”，可用于过滤器等扩展接口，实现多一个接口多个实现类
 * 2.缓存以及加载过的类，以便下次使用,缓存必须具备有可见性和线程安全；
 * 3.缓存以及创建过的类加载器；
 
 
 
 
 ### java spi的具体约定如下 ：
 
 当服务的提供者，提供了服务接口的一种实现之后，在jar包的META-INF/services/目录里同时创建一个以服务接口命名的文件。该文件里就是实现该服务接口的具体实现类。而当外部程序装配这个模块的时候，就能通过该jar包META-INF/services/里的配置文件找到具体的实现类名，并装载实例化，完成模块的注入。 
 基于这样一个约定就能很好的找到服务接口的实现类，而不需要再代码里制定。
 jdk提供服务实现查找的一个工具类：java.util.ServiceLoader
 
 
 ### 用法：
    1.将扩展类配置 "META-INF/extension" 放置于类路径classpath下
    2.在 "META-INF/extension"添加扩展说明
    3.在 需要获取扩展实现的地方使用扩展加载器加载，并通过扩展点的接口访问
 
 
 
 ```java
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
```
    
