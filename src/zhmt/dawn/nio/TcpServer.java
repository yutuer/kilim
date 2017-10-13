package zhmt.dawn.nio;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import kilim.Pausable;
import kilim.Task;

public abstract class TcpServer implements NioEventHandler {
	private String ip;
	private int port;
	private boolean started;
	private ServerSocketChannel ssc;
	private SelectionKey key;
	private NioMainLoop nioLoop;

	private final WorkTask worker;

	public TcpServer(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.worker = new WorkTask();
	}

	public void start() {
		if (started)
			return;
		started = true;
		worker.start();
		nioLoop = NioMainLoop.getTlMainLoop();
	}

	private class WorkTask extends Task {
		@Override
		public void execute() throws Pausable, Exception {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ServerSocket ss = ssc.socket();
			ss.setReuseAddress(true);
			InetSocketAddress address = new InetSocketAddress(ip, port);
			ss.bind(address);
			nioLoop.registerEventHandler(TcpServer.this,
					SelectionKey.OP_ACCEPT);
			System.out.println("Server started on : "+ip+":"+port);
		}
	}

	@Override
	public SelectableChannel getChannel() {
		return ssc;
	}

	@Override
	public void setSelectionKey(SelectionKey key) {
		this.key = key;
	}

	@Override
	public SelectionKey getSelectionKey() {
		return key;
	}

	public void onNioEvent() {
		try {
			if ((key.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				sc.socket().setTcpNoDelay(true);
				TcpChannel ch = new TcpChannel(sc);
				nioLoop.regsterTcpSocketChannel(ch);
				onAccepted(ch);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected abstract void onAccepted(TcpChannel ch);
}
