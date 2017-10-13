package zhmt.dawn.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import kilim.Pausable;
import kilim.Task;
import zhmt.dawn.concurrent.FiberSignal;
import zhmt.dawn.concurrent.FiberSwitch;

public class TcpClientChannel extends TcpChannel {
	private String ip;
	private int port;
	private InetSocketAddress addr;
	private boolean autoReconnect = true;
	private long reconnectSleep = 2000;

	private boolean connecting = false;
	private FiberSwitch channelState = new FiberSwitch(false);
	private FiberSignal connectSignal = new FiberSignal();

	public TcpClientChannel(String ip, int port, boolean autoReconnect) {
		super();
		this.ip = ip;
		this.port = port;
		this.autoReconnect = autoReconnect;

		tryReconnect(0);
	}

	private void createAndInitChannel() {
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			addr = new InetSocketAddress(ip, port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onConnectionBroken() {
		System.out.println("connecting0"+connecting);
		if (connecting) {
			return;
		} else {
			if (sc == null || sc.isConnected() || sc.socket().isClosed()
					|| sc.socket().isInputShutdown()
					|| sc.socket().isOutputShutdown()) {
				closeChannelSilently(sc);
			}
			channelState.turnOff();
		}

		if (autoReconnect)
			tryReconnect(reconnectSleep);
	}
	
	public void disconnectAndReconnect() {
		super.close();
	}

	private void tryReconnect(long delay) {
		System.out.println("connecting1"+connecting);
		if (connecting) {
			return;
		}
		
		connecting = true;
		System.out.println("connecting2"+connecting);

		new ReconnectTask(delay).start();
	}

	@Override
	public void checkConnected(long timeout) throws Pausable {
		super.checkConnected(timeout);
		if (timeout > 0)
			channelState.waitForTurningOn(timeout);
		else
			channelState.waitForTurningOn();
	}

	private class ReconnectTask extends Task {
		private long delay;

		public ReconnectTask(long delay) {
			this.delay = delay;
		}

		public void execute() throws kilim.Pausable, Exception {
			while (true) {
				System.out.println("reconnect looop" + ip+":"+port);
				if (delay > 0) {
					sleep(delay);
				}

				if (sc == null || sc.socket().isClosed()) {
					createAndInitChannel();
				}

				System.out.println("reconnect to " + ip+":"+port);
				
				//sync connect
				boolean suc = false;
				try {
					suc = sc.connect(addr);
				} catch (IOException e) {
					if (!autoReconnect) {
						failFinishConnect(new RuntimeException(e));
					} else {
						takeNap();
						continue;
					}
				}
				if (suc) {
					sucFinishConnect();
					return;
				}

				//do async connect
				NioMainLoop.getTlMainLoop().registerEventHandler(
						TcpClientChannel.this, SelectionKey.OP_CONNECT);
				connectSignal.waitForSignal();
				try {
					suc = sc.finishConnect();
				} catch (Exception e) {
					suc = false;
				}
				if (suc) {
					sucFinishConnect();
					return;
				} else {
					if (!autoReconnect)
						failFinishConnect(new RuntimeException(String.format(
								"Connecting to %s:%d failed", ip, port)));
					takeNap();
					continue;
				}
			}
		};

		private void failFinishConnect(RuntimeException e) {
			connecting = false;
			throw e;
		}

		private void sucFinishConnect() {
			System.out.println(String.format("connected to %s:%d.",ip,port));
			connecting = false;
			channelState.turnOn();
		}

		private void takeNap() throws Pausable {
			sleep(reconnectSleep);
		}
	}

	@Override
	public void close() {
		this.autoReconnect = false;
		super.close();
	}
	
	@Override
	public void onNioEvent() {
		if ((key.readyOps() & SelectionKey.OP_CONNECT) != 0) {
			key.interestOps(key.interestOps() & (~key.OP_CONNECT));
			this.connectSignal.signalFirst();
			return;
		}
		super.onNioEvent();
	}

}
