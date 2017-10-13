package zhmt.dawn.util;

import java.util.ArrayList;
import java.util.Collections;

import zhmt.dawn.TimerExe;

/**
 * 级联时间轮定时器
 * 
 * @author zhmt
 * @createdate 2015年6月12日 下午3:17:57
 * @param <E>
 */
public class CascadeTimerWheel<E extends TimerExe> {
	private Object[] wheels = new Object[5];
	public final long tickPeriod;
	private long tickStamp;

	public CascadeTimerWheel() {
		initWheels(new int[] { 500, 64, 64, 64, 64 });
		tickPeriod = 1000 / ((Wheel)wheels[0]).ticks.size();
		tickStamp = getStdTime(System.currentTimeMillis());
	}

	public static long moveCount = 0;

	public boolean canTick() {
		boolean ret = System.currentTimeMillis() - tickPeriod >= tickStamp;
		return ret;
	}

	long getStdTime(long time) {
		return time - time % tickPeriod;
	}

	public void tick() {
		tickStamp += tickPeriod;
		Wheel w = ((Wheel)wheels[0]);
		w.tick();
	}

	public E pollFirst() {
		Wheel w = ((Wheel)wheels[0]);
		E ret = w.ticks.get(w.curTick).removeFirst();
		return ret;
	}

	private int timeToTickIndex(long time) {
		long idx = (time - tickStamp);
		if (idx < 0) {
			idx = 0;
		} else {
			idx /= tickPeriod;
		}
		return (int) idx;
	}

	private int toWheelTickBlockIndex(Wheel w, long idx) {
		if (w.min > 0)
			idx = idx / w.min;
		idx += w.curTick;
		idx %= w.ticks.size();
		return (int) idx;
	}

	public void add(E e) {
		long idx = timeToTickIndex(e.getExeTime());
		Wheel w;
		for (int i = 0; i < wheels.length; i++) {
			w = (Wheel)wheels[i];
			if (idx < w.max) {
				idx = toWheelTickBlockIndex(w, idx);
				w.ticks.get((int) idx).add(e);
				return;
			}
		}
		throw new RuntimeException("Time is too long to create a timer.");
	}

	private void initWheels(int[] ticksNums) {
		ArrayList<Wheel> tmp = new ArrayList<Wheel>(ticksNums.length);
		for (int i = ticksNums.length - 1, widx = 0; i >= 0; i--, widx++) {
			tmp.add(new Wheel(ticksNums[i], widx > 0 ? tmp.get(widx - 1) : null));
		}
		Collections.reverse(tmp);

		for (int i = 0; i < tmp.size(); i++) {
			tmp.get(i).min = (i == 0 ? 0 : tmp.get(i - 1).max);
			long tickSize = 1;
			for (int n = 0; n < i; n++) {
				tickSize = tickSize * tmp.get(n).ticks.size();
			}
			tmp.get(i).max = tmp.get(i).ticks.size() * tickSize;
		}

		for (int i = 0; i < wheels.length; i++) {
			wheels[i] = tmp.get(i);
		}
	}

	public void dump() {
		System.out.println("==============");
		for (Object w : wheels) {
			System.out.println(w);
		}
	}

	private class Wheel {
		ArrayList<LinkedArrayList<E>> ticks;
		Wheel parent;
		private int curTick;
		long min;//include
		long max;//exclude

		public Wheel(int tickNum, Wheel parent) {
			ticks = new ArrayList<LinkedArrayList<E>>(tickNum);
			this.parent = parent;
			for (int i = 0; i < tickNum; i++) {
				ticks.add(new LinkedArrayList<E>());
			}
		}

		public void tick() {
			curTick++;
			if (curTick >= ticks.size()) {
				curTick = 0;
				if (parent != null) {
					parent.tick();
					LinkedArrayList<E> parentTick = parent.ticks
							.get(parent.curTick);

					E e = null;
					long idx;
					while ((e = parentTick.removeFirst()) != null) {
						idx = timeToTickIndex(e.getExeTime());
						idx = toWheelTickBlockIndex(this, idx);

						ticks.get((int) idx).add(e);
						moveCount++;
					}
				}
			}
		}

		@Override
		public String toString() {
			return "Wheel [ticks=" + ticks + ",hashCode=" + hashCode()
					+ ", curTick=" + curTick + ", parent="
					+ (parent == null ? null : parent.hashCode()) + ", min="
					+ min + ", max=" + max + "]";
		}

	}

	//	Simple

	public static void main(String[] args) throws InterruptedException {
//		CascadeTimerWheel<TimerExe> t = new CascadeTimerWheel<TimerExe>();
//		for (int i = 0; i < 16; i++) {
//			final int tmpi = i;
//			final long tmp = i * 1000 + System.currentTimeMillis();
//			t.add(new TimerExe() {
//				@Override
//				public long getExeTime() {
//					return tmp;
//				}
//
//				@Override
//				public String toString() {
//					return "" + tmpi;
//				}
//
//				@Override
//				public Type getType() {
//					// TODO Auto-generated method stub
//					return null;
//				}
//			});
//		}
//
//		t.dump();
//
//		while (true) {
//			do {
//				TimerExe hi = t.pollFirst();
//				while (hi != null) {
//					System.out.println(hi);
//					hi = t.pollFirst();
//				}
//				if (t.canTick()) {
//					t.tick();
//					if (((Wheel)(t.wheels[0])).curTick == 0) {
//						t.dump();
//					}
//				} else {
//					break;
//				}
//			} while (true);
//
//			//					System.out.println("sleep");
//			Thread.sleep(t.tickPeriod);
//
//		}
	}
}
