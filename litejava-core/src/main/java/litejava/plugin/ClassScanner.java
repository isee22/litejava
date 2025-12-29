package litejava.plugin;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

/**
 * 类扫描工具 - 统一扫描 classpath，缓存结果避免重复扫描
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 获取共享实例
 * ClassScanner scanner = ClassScanner.getInstance();
 * 
 * // 扫描指定包
 * List<Class<?>> classes = scanner.scan("com.example.controller");
 * 
 * // 按注解过滤
 * List<Class<?>> controllers = scanner.scan("com.example", RestController.class);
 * }</pre>
 */
public class ClassScanner {
    
    private static final ClassScanner INSTANCE = new ClassScanner();
    
    /** 已扫描的包（避免重复扫描） */
    private final Set<String> scannedPackages = new HashSet<>();
    
    /** 扫描结果缓存：包名 -> 类列表 */
    private final Map<String, List<Class<?>>> packageCache = new HashMap<>();
    
    private ClassScanner() {
    }
    
    public static ClassScanner getInstance() {
        return INSTANCE;
    }
    
    /**
     * 扫描指定包下的所有类
     */
    public List<Class<?>> scan(String packageName) {
        if (!scannedPackages.contains(packageName)) {
            doScan(packageName);
            scannedPackages.add(packageName);
        }
        
        List<Class<?>> result = new ArrayList<>();
        for (Map.Entry<String, List<Class<?>>> entry : packageCache.entrySet()) {
            if (entry.getKey().startsWith(packageName) || entry.getKey().equals(packageName)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * 扫描指定包下带有指定注解的类
     */
    public List<Class<?>> scan(String packageName, Class<? extends Annotation> annotation) {
        List<Class<?>> all = scan(packageName);
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : all) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }
    
    /**
     * 扫描指定包下实现/继承指定类型的类
     */
    public List<Class<?>> scan(String packageName, Class<?> superType, boolean dummy) {
        List<Class<?>> all = scan(packageName);
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : all) {
            if (superType.isAssignableFrom(clazz) && clazz != superType) {
                result.add(clazz);
            }
        }
        return result;
    }
    
    private void doScan(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    File directory = new File(resource.toURI());
                    scanDirectory(directory, packageName);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanJar(resource, packageName);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to scan package: " + packageName + " - " + e.getMessage());
        }
    }
    
    private void scanJar(URL jarUrl, String packageName) {
        try {
            String jarPath = jarUrl.getPath();
            // jar:file:/path/to/file.jar!/package/path
            int bangIndex = jarPath.indexOf("!");
            if (bangIndex > 0) {
                jarPath = jarPath.substring(5, bangIndex); // remove "file:" prefix
            }
            
            String packagePath = packageName.replace('.', '/');
            java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath);
            Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entryName.contains("$")) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    String entryPackage = className.substring(0, className.lastIndexOf('.'));
                    
                    List<Class<?>> classes = packageCache.computeIfAbsent(entryPackage, k -> new ArrayList<>());
                    try {
                        Class<?> clazz = Class.forName(className, false, 
                            Thread.currentThread().getContextClassLoader());
                        classes.add(clazz);
                    } catch (Throwable e) {
                        // 类加载失败（依赖缺失等），跳过该类，不影响其他类扫描
                    }
                }
            }
            jarFile.close();
        } catch (Exception e) {
            System.err.println("Failed to scan JAR: " + jarUrl + " - " + e.getMessage());
        }
    }
    
    private void scanDirectory(File directory, String packageName) {
        if (!directory.exists()) return;
        
        File[] files = directory.listFiles();
        if (files == null) return;
        
        List<Class<?>> classes = packageCache.computeIfAbsent(packageName, k -> new ArrayList<>());
        
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName + "." + file.getName();
                scanDirectory(file, subPackage);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className, false, 
                        Thread.currentThread().getContextClassLoader());
                    classes.add(clazz);
                } catch (Throwable e) {
                    // 类加载失败（依赖缺失等），跳过该类，不影响其他类扫描
                }
            }
        }
    }
    
    /**
     * 清除缓存（测试用）
     */
    public void clear() {
        scannedPackages.clear();
        packageCache.clear();
    }
}
