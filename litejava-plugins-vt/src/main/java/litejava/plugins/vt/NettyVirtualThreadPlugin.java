package litejava.plugins.vt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import litejava.Context;
import litejava.exception.LiteJavaException;
import litejava.plugin.ServerPlugin;

import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * Netty HTTP 服务器插件 - 虚拟线程版本 (Java 21+)
 * 使用虚拟线程作为 EventLoop 执行器
 */
public class NettyVirtualThreadPlugin extends ServerPlugin {
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;
    
    private final ObjectPool<Context> contextPool = new ObjectPool<>(
        Context::new,
        ctx -> {
            ctx.app = null;
            ctx.method = null;
            ctx.path = null;
            ctx.query = null;
            ctx.remoteAddr = null;
            ctx.wildcardPath = null;
            ctx.headers.clear();
            ctx.params.clear();
            ctx.queryParams.clear();
            ctx.state.clear();
        }
    );
    
    @Override
    public void start() {
        // 使用虚拟线程工厂
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("netty-vt-", 0)
            .factory();
        
        // Boss 用 1 个线程，Worker 用虚拟线程
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(0, virtualThreadFactory);
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, backlog)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(maxRequestSize))
                            .addLast(new RequestHandler());
                    }
                });
            
            channel = b.bind(host, app.port).sync().channel();
            app.log.info("Netty (Virtual Thread EventLoop) started on " + host + ":" + app.port);
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
    
    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext nettyCtx, FullHttpRequest request) {
            Context ctx = contextPool.acquire();
            ctx.app = app;
            
            try {
                parseRequest(request, ctx);
                app.handle(ctx);
            } catch (Exception e) {
                app.handleError(ctx, e);
            }
            
            sendResponse(nettyCtx, ctx);
            contextPool.release(ctx);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }
    
    private void parseRequest(FullHttpRequest request, Context ctx) {
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
        if (content.readableBytes() > 0) {
            byte[] body = new byte[content.readableBytes()];
            content.readBytes(body);
            ctx.setRequestBody(body);
        }
    }
    
    private void sendResponse(ChannelHandlerContext nettyCtx, Context ctx) {
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
        
        nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
