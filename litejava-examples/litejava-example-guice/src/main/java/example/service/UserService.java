package example.service;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    
    List<Map<String, Object>> list();
    
    Map<String, Object> get(long id);
    
    Map<String, Object> create(Map<String, Object> data);
    
    Map<String, Object> update(long id, Map<String, Object> data);
    
    boolean delete(long id);
}
