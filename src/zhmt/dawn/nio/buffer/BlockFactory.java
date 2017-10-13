package zhmt.dawn.nio.buffer;


import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import zhmt.dawn.util.SimpleStack;
import zhmt.dawn.util.UnsafeUtil;

class Addr {
	long addr;

	public Addr(long addr) {
		this.addr = addr;
	}
}

public interface BlockFactory<BLOCK> {
	/**
	 * will be add up to 2^n.
	 */
	public static final int BLOCK_SIZE = 4*1024;// 2^n

	public void setDebug();

	BLOCK createBlock();

	void returnBlock(BLOCK block);

	public long blockSize();

	/**
	 * CachedFactory is created to reuse memory blocks ,and share them between
	 * deferent connections. <br>
	 * Released memory will be cached here, and will passed to server if nessary.
	 * 
	 * @NotThreadSafe Should be used in single thread.
	 * @createdate 2014-6-26 下午6:39:52
	 * @param <B>
	 */
	public abstract class AbstractCachedFactory<B> implements BlockFactory<B> {
		protected long blocksize = 0;
		protected boolean debug = false;
		private SimpleStack<B> list = new SimpleStack<B>();

		public AbstractCachedFactory(long blocksize) {
			blocksize = (int) EzMath.upperPowerOfTwo(blocksize);
			this.blocksize = blocksize;
			if (this.blocksize == 0) {
				throw new IllegalArgumentException("blocksize " + blocksize
						+ " should be 2^n");
			}
		}

		@Override
		public B createBlock() {
			B ret = list.pop();
			if (ret!=null) {
				if (debug)
					System.out.println("]->");
				return ret;
			}
			if (debug)
				System.out.println("+");
			return create();
		}

		@Override
		public void returnBlock(B block) {
			if (block == null) {
				return;
			}
			if (debug)
				System.out.println("->]");
			list.push(block);
		}

		public long blockSize() {
			return blocksize;
		}

		public void setBlockSize(long size) {
			this.blocksize = size;
		}

		abstract B create();

		public void setDebug() {
			debug = true;
		}
	}

	public class CachedByteFactory extends AbstractCachedFactory<byte[]> {
		public CachedByteFactory(long blocksize) {
			super(blocksize);
		}

		@Override
		byte[] create() {
			return new byte[(int) blocksize];
		}
	}

	public class CachedDirectFactory extends AbstractCachedFactory<ByteBuffer> {
		public CachedDirectFactory(int blocksize) {
			super(blocksize);
		}

		@Override
		ByteBuffer create() {
			ByteBuffer ret = ByteBuffer.allocateDirect((int) blocksize);
			ret.limit(ret.capacity());
			return ret;
		}
	}

	public class CachedBigMemFactory extends AbstractCachedFactory<Addr> {
		public CachedBigMemFactory(long blocksize) {
			super(blocksize);
		}

		@Override
		Addr create() {
			long addr = UnsafeUtil.unsafe.allocateMemory(blocksize);
			return new Addr(addr);
		}
	}
}

