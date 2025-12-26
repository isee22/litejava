package litejava.plugins.vt;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Go 风格 JSON 工具类 - 对标 Go encoding/json
 * 
 * <pre>{@code
 * // marshal - 序列化 (支持 Map, POJO, Collection)
 * byte[] bytes = GoJson.marshal(data);
 * String json = GoJson.marshalString(data);
 * 
 * // unmarshal - 反序列化
 * User user = GoJson.unmarshal(json, User.class);
 * Map<String, Object> map = GoJson.unmarshal(json);
 * }</pre>
 */
public final class GoJson {
    
    private GoJson() {}
    
    // ThreadLocal 复用 StringBuilder (增大初始容量)
    private static final ThreadLocal<StringBuilder> BUFFER = ThreadLocal.withInitial(
        () -> new StringBuilder(4096)
    );
    
    // ThreadLocal 复用解析用的 StringBuilder
    private static final ThreadLocal<StringBuilder> PARSE_BUFFER = ThreadLocal.withInitial(
        () -> new StringBuilder(256)
    );
    
    // ========== marshal (序列化) ==========
    
    /**
     * 序列化为 byte[] (对标 Go json.Marshal)
     */
    public static byte[] marshal(Object v) {
        return marshalString(v).getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 序列化为 String
     */
    public static String marshalString(Object v) {
        StringBuilder sb = BUFFER.get();
        sb.setLength(0);
        writeValue(sb, v);
        return sb.toString();
    }
    
    // ========== unmarshal (反序列化) ==========
    
    /**
     * 反序列化为指定类型 (对标 Go json.Unmarshal)
     */
    public static <T> T unmarshal(String data, Class<T> clazz) {
        Object parsed = parseValue(data.trim(), new int[]{0});
        return convertTo(parsed, clazz);
    }
    
    /**
     * 反序列化为指定类型
     */
    public static <T> T unmarshal(byte[] data, Class<T> clazz) {
        return unmarshal(new String(data, StandardCharsets.UTF_8), clazz);
    }
    
    /**
     * 反序列化为 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> unmarshal(String data) {
        return (Map<String, Object>) parseValue(data.trim(), new int[]{0});
    }
    
    /**
     * 反序列化为 Map
     */
    public static Map<String, Object> unmarshal(byte[] data) {
        return unmarshal(new String(data, StandardCharsets.UTF_8));
    }
    
    // ========== 类型转换 ==========
    
    @SuppressWarnings("unchecked")
    private static <T> T convertTo(Object parsed, Class<T> clazz) {
        if (parsed == null) return null;
        
        // 基本类型直接返回
        if (clazz == String.class) return (T) parsed.toString();
        if (clazz == Integer.class || clazz == int.class) return (T) Integer.valueOf(((Number) parsed).intValue());
        if (clazz == Long.class || clazz == long.class) return (T) Long.valueOf(((Number) parsed).longValue());
        if (clazz == Double.class || clazz == double.class) return (T) Double.valueOf(((Number) parsed).doubleValue());
        if (clazz == Float.class || clazz == float.class) return (T) Float.valueOf(((Number) parsed).floatValue());
        if (clazz == Boolean.class || clazz == boolean.class) return (T) parsed;
        if (clazz == Map.class) return (T) parsed;
        
        // POJO 对象 - 反射赋值
        if (parsed instanceof Map) {
            return mapToObject((Map<String, Object>) parsed, clazz);
        }
        
        return (T) parsed;
    }
    
    private static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                
                String name = field.getName();
                if (map.containsKey(name)) {
                    field.setAccessible(true);
                    Object value = map.get(name);
                    field.set(obj, convertFieldValue(value, field.getType()));
                }
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshal to " + clazz.getName(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Object convertFieldValue(Object value, Class<?> type) {
        if (value == null) return null;
        if (type.isInstance(value)) return value;
        
        if (value instanceof Number) {
            Number num = (Number) value;
            if (type == int.class || type == Integer.class) return num.intValue();
            if (type == long.class || type == Long.class) return num.longValue();
            if (type == double.class || type == Double.class) return num.doubleValue();
            if (type == float.class || type == Float.class) return num.floatValue();
            if (type == short.class || type == Short.class) return num.shortValue();
            if (type == byte.class || type == Byte.class) return num.byteValue();
        }
        
        if (value instanceof String && type != String.class) {
            String str = (String) value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(str);
            if (type == long.class || type == Long.class) return Long.parseLong(str);
            if (type == double.class || type == Double.class) return Double.parseDouble(str);
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(str);
        }
        
        // 嵌套对象
        if (value instanceof Map && !Map.class.isAssignableFrom(type)) {
            return mapToObject((Map<String, Object>) value, type);
        }
        
        return value;
    }
    
    // ========== 序列化实现 ==========
    
    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value);
        } else if (value instanceof Collection) {
            writeCollection(sb, (Collection<?>) value);
        } else if (value.getClass().isArray()) {
            writeArray(sb, value);
        } else {
            // POJO - 反射序列化
            writeObject(sb, value);
        }
    }
    
    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
    
    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }
    
    private static void writeCollection(StringBuilder sb, Collection<?> coll) {
        sb.append('[');
        boolean first = true;
        for (Object item : coll) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }
    
    private static void writeArray(StringBuilder sb, Object arr) {
        sb.append('[');
        int len = java.lang.reflect.Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, java.lang.reflect.Array.get(arr, i));
        }
        sb.append(']');
    }
    
    private static void writeObject(StringBuilder sb, Object obj) {
        sb.append('{');
        boolean first = true;
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) continue; // 跳过 null (Go 默认 omitempty)
                
                if (!first) sb.append(',');
                first = false;
                writeString(sb, field.getName());
                sb.append(':');
                writeValue(sb, value);
            } catch (Exception e) {
                // skip
            }
        }
        sb.append('}');
    }
    
    // ========== 反序列化实现 ==========
    
    private static Object parseValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) return null;
        
        char c = json.charAt(pos[0]);
        switch (c) {
            case '{': return parseObject(json, pos);
            case '[': return parseArray(json, pos);
            case '"': return parseString(json, pos);
            case 't': case 'f': return parseBoolean(json, pos);
            case 'n': return parseNull(json, pos);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return parseNumber(json, pos);
                }
                throw new IllegalArgumentException("Unexpected char: " + c + " at " + pos[0]);
        }
    }
    
    private static Map<String, Object> parseObject(String json, int[] pos) {
        Map<String, Object> map = new LinkedHashMap<>(16); // 预设容量
        pos[0]++; // skip '{'
        skipWhitespace(json, pos);
        
        if (json.charAt(pos[0]) == '}') {
            pos[0]++;
            return map;
        }
        
        while (true) {
            skipWhitespace(json, pos);
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            pos[0]++; // skip ':'
            Object value = parseValue(json, pos);
            map.put(key, value);
            
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == '}') {
                pos[0]++;
                break;
            }
            pos[0]++; // skip ','
        }
        return map;
    }
    
    private static List<Object> parseArray(String json, int[] pos) {
        List<Object> list = new ArrayList<>(16); // 预设容量
        pos[0]++; // skip '['
        skipWhitespace(json, pos);
        
        if (json.charAt(pos[0]) == ']') {
            pos[0]++;
            return list;
        }
        
        while (true) {
            list.add(parseValue(json, pos));
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == ']') {
                pos[0]++;
                break;
            }
            pos[0]++; // skip ','
        }
        return list;
    }
    
    private static String parseString(String json, int[] pos) {
        pos[0]++; // skip '"'
        int start = pos[0];
        
        // 快速路径：无转义字符
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '"') {
                String result = json.substring(start, pos[0]);
                pos[0]++;
                return result;
            }
            if (c == '\\') break; // 有转义，走慢路径
            pos[0]++;
        }
        
        // 慢路径：有转义字符，使用 ThreadLocal StringBuilder
        StringBuilder sb = PARSE_BUFFER.get();
        sb.setLength(0);
        sb.append(json, start, pos[0]);
        
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]++);
            if (c == '"') break;
            if (c == '\\') {
                c = json.charAt(pos[0]++);
                switch (c) {
                    case '"': case '\\': case '/': sb.append(c); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(json.substring(pos[0], pos[0] + 4), 16));
                        pos[0] += 4;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static Number parseNumber(String json, int[] pos) {
        int start = pos[0];
        boolean isFloat = false;
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '.' || c == 'e' || c == 'E') isFloat = true;
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos[0]++;
            } else {
                break;
            }
        }
        String num = json.substring(start, pos[0]);
        return isFloat ? Double.parseDouble(num) : Long.parseLong(num);
    }
    
    private static Boolean parseBoolean(String json, int[] pos) {
        if (json.startsWith("true", pos[0])) {
            pos[0] += 4;
            return Boolean.TRUE;
        }
        pos[0] += 5;
        return Boolean.FALSE;
    }
    
    private static Object parseNull(String json, int[] pos) {
        pos[0] += 4;
        return null;
    }
    
    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && json.charAt(pos[0]) <= ' ') {
            pos[0]++;
        }
    }
}
