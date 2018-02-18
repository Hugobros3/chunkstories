package io.xol.chunkstories.client.net.netty;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.client.net.ConnectionSequence;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.LogicalPacketDatagram;
import io.xol.chunkstories.net.PacketsContextCommon;
import io.xol.chunkstories.net.netty.PacketHeaderDecoder;
import io.xol.engine.misc.ConnectionStep;

/** Handles a connection to a remote server using TCP */
public class NettyTCPServerConnection extends ServerConnection {
	private final ConnectionSequence connectionSequence;
	
	private ChannelFuture f;
	private NioEventLoopGroup workerGroup;
	
	AtomicBoolean connectOnce = new AtomicBoolean(false);
	AtomicBoolean stopOnce = new AtomicBoolean(false);
	
	public NettyTCPServerConnection(ConnectionSequence connectionSequence, ClientInterface client, String remoteAddress, int port) {
		super(client, remoteAddress, port);
		this.connectionSequence = connectionSequence;
	}
	
	public boolean connect() {
		if(!connectOnce.compareAndSet(false, true))
			return false;
		
		workerGroup = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				// Splits incomming stream into bits by looking for line-return characters
				ch.pipeline().addLast("headerDecoder", new PacketHeaderDecoder(NettyTCPServerConnection.this));
				
				// Encode outgoing strings as utf-8
				//ch.pipeline().addFirst("textPacketEncoder", new TextPacketEncoder());
			}
		});
		
		try {
			f = b.connect(remoteAddress, port).sync();
			return true;
		} catch (Exception e) {
			logger.info("Caught exception " + e + " connecting to "+remoteAddress+":"+port);
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean close() {
		if(!stopOnce.compareAndSet(false, true))
			return false;
		
		try {
			f.channel().close().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		workerGroup.shutdownGracefully();
		return true;
	}

	@Override
	public void sendTextMessage(String string) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RemoteServer getRemoteServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConnectionStep obtainModFile(String modMd5Hash, File cached) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushPacket(Packet packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PacketsContextCommon getPacketsContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void handleDatagram(LogicalPacketDatagram datagram) throws IOException, PacketProcessingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean handleSystemRequest(String msg) {
		// TODO Auto-generated method stub
		return false;
	}
}
