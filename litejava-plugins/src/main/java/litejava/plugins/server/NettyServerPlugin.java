package litejava.plugins.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import litejava.*;
import litejava.exception.LiteJavaException;
import litejava.plugin.ServerPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Netty HTTP 服务器插件 - 高性能异步服务器
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.netty</groupId>
 *     <artifactId>netty-all</artifactId>
 *     <version>4.1.100.Final</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * server.port=8080
 * server.host=0.0.0.0
 * server.threads.boss=1
 * server.threads.worker=0       # 0 = CPU cores * 2
 * server.maxRequestSize=10485760
 * server.backlog=1024
 * }</pre>
 * 
 * <h2>使用</h2>
 * <pre>{@code
 * app.use(new NettyServerPlugin());
 * }</pre>
 */
public class NettyServerPlugin extends ServerPlugin {
    
    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;
    public Channel channel;
    
    // Netty 特有配置
    public int bossThreads = 1;
    public int workerThreads = 0;  // 0 = Netty 默认 (CPU cores * 2)
    
    // Context 对象池 (类似 Gin sync.Pool)
    private final ConcurrentLinkedQueue<Context> contextPool = new ConcurrentLinkedQueue<>();
    private static final int POOL_MAX_SIZE = 1024;
    
    protected Context acquireContext() {
        Context ctx = contextPool.poll();
        if (ctx == null) {
            ctx = new Context();
        }
        ctx.app = app;
        return ctx;
    }
    
    protected void releaseContext(Context ctx) {
        if (ctx == null || contextPool.size() >= POOL_MAX_SIZE) return;
        ctx.reset();
        contextPool.offer(ctx);
    }
    
    @Override
    public void config() {
        super.config();
        bossThreads = app.conf.getInt("server.threads", "boss", bossThreads);
        workerThreads = app.conf.getInt("server.threads", "worker", workerThreads);
    }
    
    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = workerThreads > 0 ? 
            new NioEventLoopGroup(workerThreads) : new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, backlog)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(maxRequestSize))
                            .addLast(createHandler());
                    }
                });
            
            channel = b.bind(host, app.port).sync().channel();
            app.log.info("Netty server started on " + host + ":" + app.port);
        } catch (Exception e) {
            throw new LiteJavaException("Failed to start Netty server", e);
        }
    }
    
    @Override
    public void stop() {
        if (channel != null) channel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
    
    /**
     * 创建请求处理器，子类可覆盖以实现虚拟线程版本
     */
    protected SimpleChannelInboundHandler<FullHttpRequest> createHandler() {
        return new RequestHandler();
    }
    
    /**
     * 解析 HTTP 请求
     */
    protected void parseRequest(FullHttpRequest request, Context ctx) {
        ctx.method = request.method().name();
        
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        ctx.path = decoder.path();
        ctx.query = request.uri().contains("?") ? 
            request.uri().substring(request.uri().indexOf("?") + 1) : null;
        
        for (Map.Entry<String, String> entry : request.headers()) {
            ctx.headers.put(entry.getKey(), entry.getValue());
        }
        
        decoder.parameters().forEach((k, v) -> {
            if (!v.isEmpty()) ctx.queryParams.put(k, v.get(0));
        });
        
        ByteBuf content = request.content();
        byte[] body = new byte[content.readableBytes()];
        content.readBytes(body);
        ctx.setRequestBody(body);
    }
    
    /**
     * 发送 HTTP 响应 (支持 Keep-Alive)
     */
    protected void sendResponse(ChannelHandlerContext nettyCtx, FullHttpRequest request, Context ctx) {
        byte[] body = ctx.getResponseBody();
        ByteBuf content = Unpooled.wrappedBuffer(body);
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(ctx.getResponseStatus()),
            content
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        for (Map.Entry<String, String> entry : ctx.getResponseHeaders().entrySet()) {
            response.headers().set(entry.getKey(), entry.getValue());
        }
        
        // 支持 HTTP Keep-Alive
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            nettyCtx.writeAndFlush(response);
        } else {
            nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext nettyCtx, FullHttpRequest request) {
            Context ctx = acquireContext();
            
            try {
                parseRequest(request, ctx);
                app.handle(ctx);
            } catch (Exception e) {
                app.handleError(ctx, e);
            }
            
            sendResponse(nettyCtx, request, ctx);
            releaseContext(ctx);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // 静默处理 Connection reset 等网络异常
            ctx.close();
        }
    }
}
