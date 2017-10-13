package zhmt.dawn;

public interface TimerExe {
	public long getExeTime();
	
	public Type getType();

	public enum Type {
		CallbackTimerTask, Task
	}
}
