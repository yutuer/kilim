package zhmt.dawn.concurrent;

import kilim.Pausable;
import kilim.Task;
import zhmt.dawn.util.ObjectPool;
import zhmt.dawn.util.PooledObj;
import zhmt.dawn.util.SimpleLinkedList;
import zhmt.dawn.util.TlsObjPool;

public class FiberSignal {
	public static class WaitCtx implements PooledObj {
		Task task;
		public Object attachment;

		@Override
		public void reset() {
			task = null;
			attachment = null;
		}

		private static final TlsObjPool<WaitCtx> tlsPool = new TlsObjPool<FiberSignal.WaitCtx>() {
			@Override
			protected WaitCtx create() {
				return new WaitCtx();
			}
		};
	}

	private final SimpleLinkedList<WaitCtx> waiters = new SimpleLinkedList<WaitCtx>();
	private ObjectPool<WaitCtx> ctxPool;

	public FiberSignal() {
		ctxPool = WaitCtx.tlsPool.getPool();
	}

	public int waiterNum() {
		return waiters.size();
	}

	public Object waitForSignal() throws Pausable {
		WaitCtx ctx = ctxPool.getOne();//new WaitCtx();
		ctx.task = Task.getCurrentTask();
		waiters.add(ctx);
		Task.yield();
		Object ret = ctx.attachment;
		ctxPool.returnOne(ctx);
		return ret;
	}

	public void signalFirst() {
		if (waiters.size() > 0) {
			WaitCtx t = waiters.removeFirst();
			t.task.resume();
		}
	}

	public void signalFirst(Object result) {
		if (waiters.size() > 0) {
			WaitCtx t = waiters.removeFirst();
			t.attachment = result;
			t.task.resume();
		}
	}
}
