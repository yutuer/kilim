package zhmt.dawn.concurrent;

import kilim.Pausable;
import kilim.Task;
import zhmt.dawn.util.SimpleLinkedList;

/**
 * A ReentrantLock can be used in scheduler between fibers only.
 * 
 * @author zhmt
 * @createdate 2015年6月14日 下午5:41:40
 */
public class FiberReentrantLock {
	private final SimpleLinkedList<Task> waiters = new SimpleLinkedList<>();
	private Task owner = null;
	private int ref = 0;

	public void lock() throws Pausable {
		Task curTask = Task.getCurrentTask();
		if (owner == null) {
			owner = curTask;
			ref++;
			return;
		}
		if (owner == curTask) {
			ref++;
			return;
		}
		waiters.add(Task.getCurrentTask());
		Task.yield();
	}

	public void release() throws Pausable {
		Task curTask = Task.getCurrentTask();
		if (curTask != owner) {
			return;
		}

		ref--;
		if (ref <= 0) {
			owner = waiters.size() > 0 ? waiters.removeFirst() : null;
			if (owner != null)
				owner.resume();
		}
	}

	public void tryLock(long timeout) throws Pausable {
		final Task curTask = Task.getCurrentTask();

		//could acquire lock directly.
		if (owner == null) {
			owner = curTask;
			ref++;
			return;
		}
		if (owner == curTask) {
			ref++;
			return;
		}

		//wait for lock is available, or timeout.
		waiters.add(curTask);
		final TimerResumer timerResumer = TimerResumer.timerResumerPool.getOne();
		timerResumer.curTask = curTask;
		timerResumer.start(System.currentTimeMillis() + timeout);
		Task.yield();
		if (timerResumer.timeout) {
			waiters.remove(curTask);
			TimerResumer.timerResumerPool.returnOne(timerResumer);
			throw new RuntimeException("TimeoutException");
		} else {
			timerResumer.canceled = true;
		}
	}
}
