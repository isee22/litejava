package litejava.plugin;

import java.util.Map;

import litejava.Plugin;

/**
 * 视图插件基类 - 由具体实现提供模板渲染
 */
public class ViewPlugin extends Plugin {
    
    /** 单例插件，同类型自动替换 */
    @Override
    public boolean singleton() {
        return true;
    }
    
    public ViewPlugin() {
    }
    
    public String render(String template, Map<String, Object> data) {
        throw new UnsupportedOperationException("No ViewPlugin implementation");
    }
}
