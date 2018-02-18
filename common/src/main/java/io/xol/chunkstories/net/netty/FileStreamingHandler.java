package io.xol.chunkstories.net.netty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.xol.chunkstories.net.Connection;

public class FileStreamingHandler extends ChannelInboundHandlerAdapter {

	final Connection connection;
	//ByteBuf buffer;
	
	final int length;
	int neededBytes;
	
	byte[] buffer = new byte[4096];
	
	OutputStream out;
	
	public FileStreamingHandler(Connection connection, File outputFile, int length) {
		this.connection = connection;
		
		outputFile.getParentFile().mkdirs();
		try {
			out = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		this.length = length;
		this.neededBytes = length;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		//buffer = ctx.alloc().buffer(4096);
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		//buffer.release();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf inputBuffer = (ByteBuf)msg;
		
		int availableBytes = inputBuffer.readableBytes();
		while(availableBytes > neededBytes) {
			int bytesToRead = Math.min(availableBytes, Math.min(4096, neededBytes));
			inputBuffer.readBytes(buffer, 0, bytesToRead);
			int bytesRead = bytesToRead;
			
			if(out != null)
				out.write(buffer, 0, bytesRead);
			availableBytes -= bytesRead;
			neededBytes -= bytesRead;
		}
		
		//We read everything we needed! Remove ourselves from the pipeline then
		if(neededBytes == 0) {
			if(out != null) {
				out.flush();
				out.close();
			}
		
			ctx.pipeline().remove("fileReader");
		}
		
		//Still available bytes ? Those will be assumed to be regular packets, forward them to PacketHeaderDecoder.
		if(availableBytes > 0) {
			ByteBuf forward = ctx.alloc().buffer(availableBytes);
			forward.writeBytes(inputBuffer);
			ctx.fireChannelRead(forward);
		}
		
		inputBuffer.release();
	}
}
