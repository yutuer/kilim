package zhmt.dawn.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import zhmt.dawn.util.CascadeTimerWheel;
import zhmt.dawn.util.TlsInstance;
import kilim.Pausable;
import kilim.Scheduler;
import kilim.Task;

public class NioMainLoop {
	private static final TlsInstance<NioMainLoop> tlsMainLoop = new TlsInstance<NioMainLoop>() {
		@Override
		protected NioMainLoop create() {
			System.out.println(String.format("Create mainLoop for %s.",
					Thread.currentThread()));
			NioMainLoop ret = new NioMainLoop();
			ret.start();
			return ret;
		}
	};

	public static NioMainLoop getTlMainLoop() {
		return tlsMainLoop.get();
	}

	//	private final WorkTask worker;
	private boolean started = false;
	private final Selector selector;

	private NioMainLoop() {
		//		worker = new WorkTask();
		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void start() {
		if (started)
			return;
		started = true;
		//		worker.start();
//		new HealthDaemon().start();
	}

	private HashMap<Integer, Integer> mmap = new HashMap<Integer, Integer>();
	private void selectDistributionStat(int num){
		if (mmap.get(num) != null)
			mmap.put(num, mmap.get(num) + 1);
		else
			mmap.put(num, 0);
	}

	public int loopOnce(long time) throws IOException {
		int num = selector.select(time);
		if (num <= 0) {
			blankLoop++;
			return 0;
		}
		nonblankLoopNum++;
//		selectDistributionStat(num);

		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> it = selectedKeys.iterator();

		while (it.hasNext()) {
			SelectionKey oneKey = it.next();
			it.remove();
			NioEventHandler handler = (NioEventHandler) oneKey.attachment();
			//					System.out.println(handler);
			try {
				//printKey(oneKey); 
				handler.onNioEvent();
			} catch (Exception e) {
				try {
					oneKey.channel().close();
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		}
		return num;
	}

	public long blankLoop = 0;
	public long nonblankLoopNum = 0;

	private class HealthDaemon extends Task {
		public void execute() throws Pausable, Exception {
			while (true) {
				blankLoop = 0;
				nonblankLoopNum = 0;
				mmap.clear();
				CascadeTimerWheel.moveCount = 0;
				sleep(1000);
				System.out.println(String.format("LoopHit %d : miss %d",
						nonblankLoopNum, blankLoop));
//				System.out.println(mmap);
				System.out.println(CascadeTimerWheel.moveCount);
			}
		};
	};

	public static void printKey(SelectionKey oneKey) {
		System.out
				.println(String
						.format("INTEREST connect : %d,read : %d , write : %d ,accept : %d, attchment : %s",
								oneKey.readyOps() & oneKey.OP_CONNECT,
								oneKey.readyOps() & oneKey.OP_READ,
								oneKey.readyOps() & oneKey.OP_WRITE,
								oneKey.readyOps() & oneKey.OP_ACCEPT,
								oneKey.attachment()));
	}

	public void regsterTcpSocketChannel(TcpChannel tsc) {
		registerEventHandler(tsc, 0);
	}

	public void registerEventHandler(NioEventHandler evtHandler, int events) {
		try {
			SelectionKey key = evtHandler.getChannel().register(selector,
					events);
			key.attach(evtHandler);
			evtHandler.setSelectionKey(key);
		} catch (ClosedChannelException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		Scheduler sch = Scheduler.getDefaultScheduler();

		sch.schedule(new Task() {
			@Override
			public void execute() throws Pausable, Exception {
			}
		});
	}

}
