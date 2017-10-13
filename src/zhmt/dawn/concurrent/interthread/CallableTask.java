package zhmt.dawn.concurrent.interthread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import kilim.Pausable;
import kilim.Task;

/**
 * Post a task from a non-schduler thread to a scheduler,and wait for the result
 * until the task is done.
 * 
 * @author zhmt
 * @createdate 2015年6月22日 上午8:08:10
 */
public class CallableTask<RET> {
	private final AtomicReference<Object> result = new AtomicReference<>();
	private final CountDownLatch latch = new CountDownLatch(1);
	private final Task task = new FiberTask();

	public RET execute() throws Pausable, Exception {
		return null;
	}

	private RET getResult() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		Object obj = result.get();
		if (obj instanceof RuntimeException) {
			throw (RuntimeException) obj;
		} else if (obj instanceof Exception) {
			throw new RuntimeException((Exception) obj);
		} else {
			return (RET) obj;
		}
	}

	public RET startOn() {
		this.task.start();
		return getResult();
	}

	public Task getTask() {
		return task;
	}

	public void sleep(long time) throws Pausable {
		task.sleep(time);
	}

	private class FiberTask extends Task {
		@Override
		public void execute() throws Pausable, Exception {
			try {
				RET ret = CallableTask.this.execute();
				result.set(ret);
			} catch (Exception e) {
				result.set(e);
			} finally {
				latch.countDown();
			}
		}
	}
}
