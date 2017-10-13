package zhmt.dawn.concurrent;

import kilim.Task;
import zhmt.dawn.NonpausableTask;
import zhmt.dawn.util.PooledObj;
import zhmt.dawn.util.TlsObjPool;

public class TimerResumer extends NonpausableTask implements PooledObj {
	public static final TlsObjPool<TimerResumer> timerResumerPool = new TlsObjPool<TimerResumer>() {
		@Override
		protected TimerResumer create() {
			return new TimerResumer();
		}
	};

	boolean timeout = false;
	boolean canceled = false;
	Task curTask = null;

	@Override
	public void execute() {
		if (!canceled) {
			timeout = true;
			curTask.resume();
		} else {
			TimerResumer.timerResumerPool.returnOne(this);
		}
	}

	@Override
	public void reset() {
		timeout = false;
		canceled = false;
		curTask = null;
	}
}
