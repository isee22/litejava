package litejava.plugins.search;

import litejava.Plugin;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 全文搜索插件
 * 
 * <h2>配置</h2>
 * <pre>
 * elasticsearch:
 *   host: localhost
 *   port: 9200
 *   scheme: http
 * </pre>
 */
public class ElasticsearchPlugin extends Plugin {
    
    public String host = "localhost";
    public int port = 9200;
    public String scheme = "http";
    
    public RestClient restClient;
    public ElasticsearchClient client;
    
    public ElasticsearchPlugin() {
    }
    
    /**
     * 构造函数 - 指定主机
     */
    public ElasticsearchPlugin(String host) {
        this.host = host;
    }
    
    /**
     * 构造函数 - 指定主机和端口
     */
    public ElasticsearchPlugin(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 构造函数 - 指定主机、端口和协议
     */
    public ElasticsearchPlugin(String host, int port, String scheme) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        host = app.conf.getString("elasticsearch", "host", host);
        port = app.conf.getInt("elasticsearch", "port", port);
        scheme = app.conf.getString("elasticsearch", "scheme", scheme);
        
        restClient = RestClient.builder(new HttpHost(host, port, scheme)).build();;
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
        
        app.log.info("ElasticsearchPlugin: Connected to " + scheme + "://" + host + ":" + port);
    }
    
    public void index(String index, String id, Map<String, Object> document) {
        try {
            client.index(i -> i.index(index).id(id).document(document));
        } catch (Exception e) {
            throw new RuntimeException("Failed to index: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> get(String index, String id) {
        try {
            GetResponse<Map> response = client.get(g -> g.index(index).id(id), Map.class);
            return response.found() ? response.source() : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get: " + e.getMessage(), e);
        }
    }
    
    public void delete(String index, String id) {
        try {
            client.delete(d -> d.index(index).id(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete: " + e.getMessage(), e);
        }
    }
    
    public List<Map> search(String index, String field, String query) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                .index(index)
                .query(q -> q.match(m -> m.field(field).query(query))),
                Map.class
            );
            return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to search: " + e.getMessage(), e);
        }
    }
    
    public List<Map> searchMultiMatch(String index, String query, String... fields) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                .index(index)
                .query(q -> q.multiMatch(m -> m.query(query).fields(List.of(fields)))),
                Map.class
            );
            return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to search: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void uninstall() {
        try {
            if (restClient != null) restClient.close();
        } catch (Exception ignored) {}
    }
}
