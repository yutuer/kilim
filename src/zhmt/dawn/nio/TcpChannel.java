package zhmt.dawn.nio;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import kilim.Pausable;
import zhmt.dawn.concurrent.FiberReentrantLock;
import zhmt.dawn.concurrent.FiberSignal;
import zhmt.dawn.nio.buffer.ScalableBuf;
import zhmt.dawn.util.ObjectPool;
import zhmt.dawn.util.PooledObj;
import zhmt.dawn.util.TlsObjPool;

public class TcpChannel implements NioEventHandler {
	enum LinkState {
		created, connected, closed
	}

	protected SocketChannel sc;
	protected SelectionKey key;

	private FiberReentrantLock readLock = new FiberReentrantLock();
	private FiberReentrantLock writeLock = new FiberReentrantLock();

	private LinkState linkState = LinkState.created;

	private boolean hasGotEof = false;

	public TcpChannel() {
		ctxPool = IoCtx.tlsPool.getPool();
	}

	public TcpChannel(SocketChannel sc) {
		ctxPool = IoCtx.tlsPool.getPool();
		this.sc = sc;
	}

	public void close() {
		linkState = LinkState.closed;
		onConnectionBroken();
	}

	public boolean isClosed() {
		return linkState == LinkState.closed;
	}

	@Override
	public SelectableChannel getChannel() {
		return this.sc;
	}

	@Override
	public void setSelectionKey(SelectionKey key) {
		this.key = key;
	}

	@Override
	public SelectionKey getSelectionKey() {
		return key;
	}

	private final FiberSignal readableSignal = new FiberSignal();
	private final FiberSignal writableSignal = new FiberSignal();
	private ObjectPool<IoCtx> ctxPool;

	private static class IoCtx implements PooledObj {
		private boolean eof = false;
		private IOException ioException = null;

		@Override
		public void reset() {
			eof = false;
			ioException = null;
		}

		private static final TlsObjPool<IoCtx> tlsPool = new TlsObjPool<IoCtx>() {
			@Override
			protected IoCtx create() {
				return new IoCtx();
			}
		};
	}

	/**
	 * Operation read is atomic, will not be interupted by reading from other
	 * tasks.
	 * 
	 * @param buf
	 * @throws Pausable
	 */
	public int readSome(ScalableBuf buf) throws Pausable {
		readLock.lock();
		IoCtx ctx = ctxPool.getOne();
		try {
			checkConnected(-1);
			//try read syncly
			//			int n = tryToReadMaxSlilently(buf, ctx);  //TODO remove it?
			//			if (n > 0)
			//				return n;
			//			tryToThrowIoException(ctx);

			//Got nothing, do async read;
			key.interestOps(key.interestOps() | key.OP_READ);
			//			System.out.println("add OP_READ  ");
			readableSignal.waitForSignal();
			//			System.out.println("reading...");
			int count = tryToReadMaxSlilently(buf, ctx);
			if (count > 0) {
				return count;
			}
			tryToThrowIoException(ctx);
			return count;
		} finally {
			ctxPool.returnOne(ctx);
			readLock.release();
		}
	}

	public int writeAll(ScalableBuf buf) throws Pausable {
		writeLock.lock();
		try {
			checkConnected(-1);
			long n = buf.readable();
			IoCtx ctx = new IoCtx();
			//try write syncly
			tryToWriteMaxSilently(buf, ctx);
			if (buf.readable() <= 0) {
				return (int) n;
			}
			tryToThrowIoException(ctx);

			//has remaining, do async read;
			while (buf.readable() > 0) {
				key.interestOps(key.interestOps() | key.OP_WRITE);
				//				System.out.println("add OP_WRITE");
				writableSignal.waitForSignal();
				//				System.out.println("writing...");
				tryToWriteMaxSilently(buf, ctx);
				tryToThrowIoException(ctx);
			}
			return (int) n;
		} finally {
			writeLock.release();
		}
	}

	private void tryToThrowIoException(IoCtx ctx) {
		if (ctx.ioException != null) {
			throw new RuntimeException(ctx.ioException);
		}
		if (ctx.eof) {
			if (hasGotEof)
				throw new RuntimeException("EOF");
			else
				hasGotEof = true;
		}
	}

	private int tryToReadMaxSlilently(ScalableBuf buf, IoCtx ctx) {
		long onceCount = 0;
		try {
			onceCount = buf.readFrom(sc);
		} catch (IOException e) {
			ctx.ioException = e;
			onConnectionBroken();
		}
		if (onceCount < 0) {
			ctx.eof = true;
		}

		return (int) onceCount;
	}

	private int tryToWriteMaxSilently(ScalableBuf buf, IoCtx ctx) {
		long count = 0;
		try {
			count = buf.writeTo(sc);
		} catch (IOException e) {
			ctx.ioException = e;
			onConnectionBroken();
		}
		return (int) count;
	}

	@Override
	public void onNioEvent() {
		//		System.out.println("onNioEvent");
		if ((key.readyOps() & SelectionKey.OP_READ) != 0) {
			key.interestOps(key.interestOps() & (~key.OP_READ));
			this.readableSignal.signalFirst();
		}

		if ((key.readyOps() & SelectionKey.OP_WRITE) != 0) {
			key.interestOps(key.interestOps() & (~key.OP_WRITE));
			this.writableSignal.signalFirst();
		}
	}

	public void checkConnected(long timeout) throws Pausable {
		if (isClosed()) {
			throw new RuntimeException("ClosedTcpChannel.");
		}
	}

	protected void onConnectionBroken() {
		closeChannelSilently(sc);
	}

	protected static void closeChannelSilently(Channel ch) {
		if (ch == null) {
			return;
		}
		try {
			ch.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
