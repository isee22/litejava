package common.vo;

import java.util.List;

/**
 * 通用列表响应
 */
public class ListResult<T> {
    public List<T> list;
    public int total;
    
    public ListResult(List<T> list) {
        this.list = list;
        this.total = list != null ? list.size() : 0;
    }
    
    public static <T> ListResult<T> of(List<T> list) {
        return new ListResult<>(list);
    }
}
