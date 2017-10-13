package zhmt.dawn.concurrent.interthread;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import kilim.Pausable;
import kilim.Scheduler;
import kilim.Task;
import zhmt.dawn.NonpausableTask;

/**
 * Post a long run task(CPU bounded) to an ThreadPool from a fiber.
 * 
 * @author zhmt
 * @createdate 2015年6月22日 下午3:42:49
 * @param <RET>
 */
public class ThreadPoolCallableTask<RET> {
	private final AtomicReference<Object> result = new AtomicReference<>();
	private final Callable<RET> task = new ThreadPoolTask();
	private Scheduler sch;
	private Task callerTask;

	public RET execute() throws Exception {
		return null;
	}

	private RET getResult() throws Pausable {
		Task.yield();
		Object obj = result.get();
		if (obj instanceof RuntimeException) {
			throw (RuntimeException) obj;
		} else if (obj instanceof Exception) {
			throw new RuntimeException((Exception) obj);
		} else {
			return (RET) obj;
		}
	}

	public RET startOn(AbstractExecutorService es) throws Pausable {
		sch = Scheduler.getDefaultScheduler();
		callerTask = Task.getCurrentTask();
		es.submit(task);
		return getResult();
	}
	
	public Callable<RET> getTask() {
		return task;
	}

	private void postWakingTask() {
		sch.schedule(new NonpausableTask() {
			@Override
			public void execute() {
				callerTask.resume();
			}
		});
	}

	private class ThreadPoolTask implements Callable<RET> {
		@Override
		public RET call() throws Exception {
			try {
				RET ret = ThreadPoolCallableTask.this.execute();
				result.set(ret);
			} catch (Exception e) {
				result.set(e);
			} finally {
				postWakingTask();
			}
			return null;
		}
	}
}
