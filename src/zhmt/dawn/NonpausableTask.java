package zhmt.dawn;

import kilim.Scheduler;

public class NonpausableTask implements TimerExe {
	public long exeTime;

	public void execute() {

	}

	@Override
	public long getExeTime() {
		return exeTime;
	}

	@Override
	public Type getType() {
		return TimerExe.Type.CallbackTimerTask;
	}

	public void start(long startTime) {
		this.exeTime = startTime;
//		Scheduler.getDefaultScheduler().schedule(this);
	}

}
