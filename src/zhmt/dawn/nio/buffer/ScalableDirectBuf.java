package zhmt.dawn.nio.buffer;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import zhmt.dawn.util.UnsafeUtil;

/**
 * Bigendian
 * 
 * @author zhmt
 */
public class ScalableDirectBuf extends ScalableBuf<ByteBuffer> {
	long[] bases = new long[50];

	private static final ThreadLocal<BlockFactory<ByteBuffer>> fact = new ThreadLocal<>();

	private static BlockFactory<ByteBuffer> getFact() {
		BlockFactory<ByteBuffer> ret = fact.get();
		if (ret == null) {
			ret = new BlockFactory.CachedDirectFactory(BlockFactory.BLOCK_SIZE);
			fact.set(ret);
		}
		return ret;
	}

	public static ScalableDirectBuf allocateFromTlsCache() {
		return new ScalableDirectBuf(getFact());
	}

	public ScalableDirectBuf(BlockFactory<ByteBuffer> blockFactory) {
		init(blockFactory);
	}

	@Override
	public int writeTo(ScalableBuf<?> buf) {
		if (buf instanceof ScalableDirectBuf) {
			ScalableDirectBuf dbuf = (ScalableDirectBuf) buf;
			int count = 0;
			while (readable() > 0) {
				long ri_ = ri;
				long rb = ri_ >>> blocksizesqrt;
				long rbi = ri_ & blocksizemask;
				long len = Math.min(blocksize - rbi, readable());
				dbuf.wbytes(bases[(int) rb], (int) rbi, (int) len);
				ri += len;
				count += len;
			}
			return count;
		}
		return 0;
	}

	public void wbytes(long dataBaseAddress, int offset, int len) {
		checkWrite(len);
		setBytes(len, (int) wi, dataBaseAddress, offset);
		wi += len;
	}

	protected void reallocateBufArray() {
		int newlen = bufs.length << 1;

		ByteBuffer[] newbufs = new ByteBuffer[newlen];
		System.arraycopy(bufs, 0, newbufs, 0, bufs.length);
		bufs = newbufs;

		long[] newbases = new long[newlen];
		System.arraycopy(bases, 0, newbases, 0, bases.length);
		bases = newbases;
	}

	@Override
	protected void setByte_(long wb, long wbi, int data) {
		unsafe.putByte(bases[(int) wb] + (wbi << 0), (byte) data);
	}

	@Override
	protected byte getByte_(long rb, long rbi) {
		byte ret = unsafe.getByte(bases[(int) rb] + rbi);
		return ret;
	}

	protected void setBytes(int bytelen, int wi, long dataBaseAddress,
			int offset) {
		long wi_ = wi;
		long remains = bytelen;

		long left = 0;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		while (remains > 0) {
			left = blocksize - wbi;
			if (left > remains) {
				unsafe.copyMemory(null, dataBaseAddress + offset
						+ ((bytelen - remains)), null, bases[(int) wb] + wbi,
						remains);
				wbi += remains;
				remains -= remains;
			} else {
				unsafe.copyMemory(null, dataBaseAddress + offset
						+ ((bytelen - remains)), null, bases[(int) wb] + wbi,
						left);
				wb++;
				wbi = 0;
				remains -= left;
			}
		}
	}

	private static final long ADDRESS_OFFSET;
	static {
		long tmp = 0;
		try {
			Field field = Buffer.class.getDeclaredField("address");
			tmp = unsafe.objectFieldOffset(field);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		ADDRESS_OFFSET = tmp;
	}

	static long getDirectAddress(ByteBuffer buf) {
		if (buf == null) {
			return 0;
		}
		long ret = unsafe.getLong(buf, ADDRESS_OFFSET);
		return ret;
	}

	@Override
	protected ByteBuffer[] createBufArray(int len) {
		return new ByteBuffer[len];
	}

	@Override
	protected void setBlock(int i, ByteBuffer block) {
		bufs[i] = block;
		if (block != null)
			bases[i] = getDirectAddress(block);
		else
			bases[i] = 0;
	}

	@Override
	protected ByteBuffer wrapBlock(long b, long position, long limit) {
		ByteBuffer tmp = bufs[(int) b];
		tmp.position((int) position);
		tmp.limit((int) limit);
		return tmp;
	}

	@Override
	protected void copyMemoryToSingleBlock(Object ojb, long address, long b,
			long bi, long mum) {
		unsafe.copyMemory(ojb, address, null, bases[(int) b] + bi, mum);
	}

	@Override
	protected void copyMemoryFromSingleBlock(long b, long bi, Object obj,
			long address, long num) {
		unsafe.copyMemory(null, bases[(int) b] + bi, obj, address, num);
	}

	@Override
	protected void setShortToSingleBlock(long wb, long wbi, short data) {
		if (!isNativeByteOrder) {
			data = Short.reverseBytes(data);
		}
		unsafe.putShort(bases[(int) wb] + wbi, data);
	}

	@Override
	protected short getShortFromSingleBlock(long rb, long rbi) {
		short data = unsafe.getShort(bases[(int) rb] + rbi);
		if (!isNativeByteOrder) {
			data = Short.reverseBytes(data);
		}
		return data;
	}

	@Override
	protected void setIntToSingleBlock(long wb, long wbi, int data) {
		if (!isNativeByteOrder) {
			data = Integer.reverseBytes(data);
		}
		unsafe.putInt(bases[(int) wb] + wbi, data);
	}

	@Override
	protected int getIntFromSingleBlock(long rb, long rbi) {
		int data = unsafe.getInt(bases[(int) rb] + rbi);
		if (!isNativeByteOrder) {
			data = Integer.reverseBytes(data);
		}
		return data;
	}

	@Override
	protected void setLongToSingleBlock(long wb, long wbi, long data) {
		if (!isNativeByteOrder) {
			data = Long.reverseBytes(data);
		}
		unsafe.putLong(bases[(int) wb] + wbi, data);
	}

	@Override
	protected long getLongFromSingleBlock(long rb, long rbi) {
		long data = unsafe.getLong(bases[(int) rb] + rbi);
		if (!isNativeByteOrder) {
			data = Long.reverseBytes(data);
		}
		return data;
	}

	public static void main(String[] args) {
		ByteBuffer buf = ByteBuffer.allocateDirect(10);
		long base = getDirectAddress(buf);
		unsafe.putByte(base, (byte) 1);
		System.out.println(unsafe.getByte(base));

		long N = 10000000000L;

		long start = System.currentTimeMillis();
		for (long i = 0; i < N; i++) {
			//			b.position(0);
			//			
			unsafe.putByte(base, (byte) 1);
			unsafe.getByte(base);
		}
		System.out.println(System.currentTimeMillis() - start);

		byte[] arr = new byte[1];
		start = System.currentTimeMillis();
		int n;
		for (long i = 0; i < N; i++) {
			n = arr[0];
			arr[0] = (byte) n;
		}
		System.out.println(System.currentTimeMillis() - start);
	}

	public void clear() {
		ri = wi;
		compact();
	}

	public void release() {
		clear();
		for (int i = 0; i < bufs.length; i++) {
			if (bufs[i] == null) {
				break;
			}
			blockFactory.returnBlock(bufs[i]);
			bufs[i] = null;
		}
	}

}
