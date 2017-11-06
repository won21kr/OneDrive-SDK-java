package com.bhyoo.onedrive.network.async;

import com.bhyoo.onedrive.utils.DirectByteInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

public class AsyncClientHandler extends SimpleChannelInboundHandler<HttpObject> {
	private final DirectByteInputStream resultStream = new DirectByteInputStream();
	private final ResponsePromise responsePromise;


	public AsyncClientHandler(ResponsePromise responsePromise) {
		this.responsePromise = responsePromise;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		responsePromise.setChannel(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		responsePromise.setFailure(cause);
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpResponse) {
			responsePromise.setResponse((HttpResponse) msg);
		}

		if (msg instanceof HttpContent) {
			HttpContent content = (HttpContent) msg;

			// read response content and copy to result future object's stream
			ByteBuf byteBuf = content.content();
			int remaining = byteBuf.readableBytes();
			resultStream.ensureRemain(remaining);
			byteBuf.readBytes(resultStream.rawBuffer(), resultStream.getIn() + 1, remaining);
			resultStream.jumpTo(resultStream.getIn() + remaining);

			// if this message is last of response
			if (content instanceof LastHttpContent) {
				ctx.close();
				responsePromise.trySuccess(resultStream);
			}
		}
	}
}