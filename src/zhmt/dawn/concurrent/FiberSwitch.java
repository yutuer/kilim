package zhmt.dawn.concurrent;

import kilim.Pausable;
import kilim.Task;
import zhmt.dawn.util.SimpleLinkedList;

public class FiberSwitch {
	private boolean on = false;
	private final SimpleLinkedList<Task> waiters = new SimpleLinkedList<Task>();

	public FiberSwitch(boolean on) {
		this.on = on;
	}

	public void waitForTurningOn() throws Pausable {
		if (this.on) {
			return;
		}
		waiters.add(Task.getCurrentTask());
		Task.yield();
	}

	public void waitForTurningOn(long timeout) throws Pausable {
		if (this.on) {
			return;
		}

		final Task curTask = Task.getCurrentTask();
		waiters.add(curTask);

		TimerResumer timer = TimerResumer.timerResumerPool.getOne();
		timer.curTask = curTask;
		timer.start(System.currentTimeMillis() + timeout);
		Task.yield();
		if (timer.timeout) {
			waiters.remove(curTask);
			TimerResumer.timerResumerPool.returnOne(timer);
			throw new RuntimeException("TimeoutException");
		} else {
			timer.canceled = true;
		}
	}

	public void turnOn() {
		if (this.on) {
			return;
		}
		this.on = true;
		if (waiters.size() <= 0)
			return;

		while (waiters.size()>0) {
			Task task = waiters.removeFirst();
			task.resume();
		}
	}

	public void turnOff() {
		this.on = false;
	}

}
