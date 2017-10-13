package zhmt.dawn.nio.rpc.qrpc;

import kilim.Pausable;
import kilim.Task;
import zhmt.dawn.concurrent.FiberSignal;
import zhmt.dawn.nio.TcpClientChannel;
import zhmt.dawn.nio.buffer.ScalableDirectBuf;

public abstract class QRpcClientChannel {
	private ScalableDirectBuf writeBuf;
	private ScalableDirectBuf readBuf;
	private long lastSendTimestamp = System.currentTimeMillis();
	private long lastRecvTimestamp = System.currentTimeMillis();
	private final FiberSignal signal = new FiberSignal();
	private TcpClientChannel channel;
	private long parseOffset = 0;
	private final long tcpDelay;

	public QRpcClientChannel(String ip, int port, boolean autoReconnect,
			long tcpDelayInMs) {
		this.tcpDelay = tcpDelayInMs;
		this.channel = new TcpClientChannel(ip, port, autoReconnect);
		writeBuf = ScalableDirectBuf.allocateFromTlsCache();
		readBuf = ScalableDirectBuf.allocateFromTlsCache();

		ReadTask rtask = new ReadTask();
		rtask.outter = this;
		rtask.start();

		WriteTask wtask = new WriteTask();
		wtask.outter = this;
		wtask.start();
	}

	public QRpcClientChannel(String ip, int port, boolean autoReconnect) {
		this(ip, port, autoReconnect, 5);// 5 is a tested value
	}

	protected abstract void serial(Object req, ScalableDirectBuf buf);

	protected abstract long isPackReceived(ScalableDirectBuf buf,
			long startRindex);

	protected abstract Object deserial(ScalableDirectBuf buf);

	public Object request(Object req) throws Pausable {
		try {
			channel.checkConnected(-1);
			serial(req, writeBuf);
			RuntimeException e = (RuntimeException) signal.waitForSignal();
			if (e != null) {
				throw e;
			}
			Object rsp = deserial(readBuf);
			compactReadBuf();
			return rsp;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class ReadTask extends Task {
		QRpcClientChannel outter;

		@Override
		public void execute() throws Pausable, Exception {
			while (!outter.channel.isClosed()) {
				if (outter.signal.waiterNum() <= 0) {
					sleep(outter.tcpDelay);
					continue;
				}
				if (System.currentTimeMillis() - outter.lastRecvTimestamp < outter.tcpDelay) {
					sleep(outter.tcpDelay);
					continue;
				}
				outter.lastRecvTimestamp = System.currentTimeMillis();

				try {
					outter.channel.readSome(outter.readBuf);
					while (outter.hasMorePacket()) {
						outter.signal.signalFirst();
					}
				} catch (Exception e) {
					e.printStackTrace();
					RuntimeException tmp = new RuntimeException(e);
					outter.clearBuffer();
					outter.wakeAll(tmp);
					continue;
				}
			}
			outter.wakeAll(new RuntimeException("tcp channel closed."));
		}
	}

	private void wakeAll(RuntimeException exception) {
		while (signal.waiterNum() > 0) {
			signal.signalFirst(exception);
		}
	}

	private static class WriteTask extends Task {
		QRpcClientChannel outter;

		@Override
		public void execute() throws Pausable, Exception {
			while (!outter.channel.isClosed()) {
				if (outter.writeBuf.readable() <= 0) {
					sleep(outter.tcpDelay);
					continue;
				}
				if (System.currentTimeMillis() - outter.lastSendTimestamp < outter.tcpDelay) {
					sleep(outter.tcpDelay);
					continue;
				}
				outter.lastSendTimestamp = System.currentTimeMillis();

				try {
					outter.channel.writeAll(outter.writeBuf);
					outter.writeBuf.compact();
				} catch (Exception e) {
					RuntimeException tmp = new RuntimeException(e);
					outter.clearBuffer();
					outter.wakeAll(tmp);
					continue;
				}
			}
			outter.wakeAll(new RuntimeException("tcp channel closed."));
		}
	}

	private void clearBuffer() {
		writeBuf.clear();
		readBuf.clear();
		parseOffset = 0;
	}

	private void compactReadBuf() {
		long biggerThanRidx = parseOffset - readBuf.ri();
		readBuf.compact();
		parseOffset = (readBuf.ri() + biggerThanRidx);
	}

	private boolean hasMorePacket() {
		long tmp = 0;
		if ((tmp = this.isPackReceived(readBuf, this.parseOffset)) >= 0) {
			this.parseOffset = tmp;
			return true;
		}
		return false;
	}

	public void close() {
		clearBuffer();
		channel.close();
	}
}
