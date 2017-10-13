package zhmt.dawn.nio.buffer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import zhmt.dawn.util.UnsafeUtil;

/**
 * Auto resized buffer.
 * 
 * TODO bigendian ,little endian support.
 * 
 * @createdate 2014-6-4 下午4:16:15
 */
@SuppressWarnings("static-access")
public abstract class ScalableBuf<BLOCK> {
	protected BlockFactory<BLOCK> blockFactory;
	protected long blocksize = 0;
	protected boolean isBigEndian = true;
	protected boolean isNativeByteOrder = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
	/**
	 * The two variables below are used for maximu using of bitwise opertations.
	 */
	protected long blocksizesqrt = 0;
	protected long blocksizemask = 0;
	BLOCK[] bufs;
	/**
	 * 当前实际容量 = blocksize*blocknum
	 */
	long limitpos;
	/**
	 * 下一个写入位置
	 */
	long wi;

	/**
	 * 下一个读取位置
	 */
	long ri;

	public void init(BlockFactory<BLOCK> blockFactory) {
		this.bufs = createBufArray(50);
		this.blocksize = blockFactory.blockSize();
		blocksizemask = blocksize - 1;

		for (int i = 0;; i++) {
			if ((1 << i) >= blocksize) {
				blocksizesqrt = i;
				break;
			}
		}

		limitpos = 0;
		wi = 0;
		ri = 0;
		this.blockFactory = blockFactory;
	}
	
	public void setByteOrder(ByteOrder order){
		isBigEndian = (order==ByteOrder.BIG_ENDIAN);
		isNativeByteOrder = (ByteOrder.nativeOrder() == order);
	}

	/**
	 * Dump the specified memory range and print it in console.
	 * 
	 * @param start
	 *            include
	 * @param end
	 *            exclude
	 */
	public void dump(long start, long end, int bytesPerLine, int radix) {
		dump(start, end, bytesPerLine, radix, System.out);
	}

	public void dump(long start, long end, int bytesPerLine, int radix,
			PrintStream ps) {
		int intwidth = Math.min(4, bytesPerLine);

		int pwidth = 0;
		if (radix < 8) {
			pwidth = 9;
		} else if (radix < 10) {
			pwidth = 5;
		} else if (radix < 16) {
			pwidth = 5;
		} else {
			pwidth = 3;
		}

		int blockIndex = 0;
		printline(bytesPerLine, pwidth, ps);
		for (int i = 0; i < bytesPerLine; i++) {
			blockIndex = printData(i, bytesPerLine, 10,
					String.format("%-10s ", "MEMDUMP"), pwidth, i, intwidth,
					ps, blockIndex);
		}
		ps.println();
		printline(bytesPerLine, pwidth, ps);

		blockIndex = 0;
		for (int i = 0; i < end; i++) {
			blockIndex = printData(i, bytesPerLine, radix,
					String.format("%010d ", start + i), pwidth,
					gbyte(start + i), intwidth, ps, blockIndex);
		}
		ps.println();
		printline(bytesPerLine, pwidth, ps);
		ps.println("\r\n");
	}

	private void printline(int bytesPerLine, int pwidth, PrintStream ps) {
		for (int i = 0; i < bytesPerLine * pwidth + 10 + 10; i++) {
			ps.print('-');
		}
		ps.println();
	}

	private int printData(int i, int bytesPerLine, int radix, String left,
			int pwidth, int data, int intwidth, PrintStream ps, int blockIndex) {
		if (i > 0 && i % bytesPerLine == 0) {
			ps.println();
		}
		if (i % bytesPerLine == 0) {
			ps.print(left);
		}
		if (blockIndex % intwidth == 0) {
			ps.print("[");
		}

		String toPrint = null;
		if (radix != 2) {
			toPrint = String.format("%" + pwidth + "s",
					Integer.toString(data, radix));
		} else {
			String t = Integer.toBinaryString(data);
			toPrint = String.format("%" + pwidth + "s",
					t.substring(0, Math.min(8, t.length())));
		}
		ps.print(toPrint);
		if (blockIndex % intwidth == intwidth - 1
				|| blockIndex == bytesPerLine - 1) {
			ps.print("]");
		}

		if (blockIndex == bytesPerLine - 1) {
			blockIndex = 0;
		} else {
			blockIndex++;
		}
		return blockIndex;
	}

	public long wi() {
		return wi;
	}

	public long blockSize() {
		return blocksize;
	}

	public long ri() {
		return ri;
	}

	public long readable() {
		return wi - ri;
	}

	public long writable() {
		return Long.MAX_VALUE - wi;
	}

	public void skipr(long n) {
		checkRead(n);
		ri += n;
	}

	public void skipw(long n) {
		checkWrite(n);
		wi += n;
	}

	public long writeTo(WritableByteChannel channel) throws IOException {
		final long max_write = blocksize * 10;
		long count = 0;
		while (readable() > 0) {
			long ri_ = ri;
			long rb = ri_ >>> blocksizesqrt;
			long rbi = ri_ & blocksizemask;
			long len = Math.min(blocksize - rbi, readable());
			ByteBuffer tmp = wrapBlock(rb, rbi, rbi + len);
			int n = channel.write(tmp);
			if (n > 0) {
				count += n;
				ri += n;
				if (tmp.remaining() > 0) {
					// 没写完，不再尝试
					break;
				}
				if (count > max_write) {
					// 给其它channel留写机会
					break;
				}
			} else {
				break;
			}
		}
		return count;
	}

	public long readFrom(ReadableByteChannel channel) throws IOException {
		final long max_write = blocksize * 10;
		long count = 0;
		long n = 0;
		do {
			checkWrite(1);
			long wi_ = wi;
			long wb = wi_ >>> blocksizesqrt;
			long wbi = wi_ & blocksizemask;

			ByteBuffer buf = wrapBlock(wb, wbi, blocksize);
			n = channel.read(buf);
			if (n < 0) {
				if (count > 0) {
					return count;
				} else {
					return n;
				}
			} else if (n > 0) {
				count += n;
				wi += n;
				if (buf.remaining() > 0) {
					break;
				}
				if (count > max_write) {
					break;
				}
			} else {
				break;
			}
		} while (n > 0);
		return count;
	}

	public abstract int writeTo(ScalableBuf<?> buf);

	/**
	 * Compact the buffer. Recyle useless memory.
	 */
	public void compact() {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		if (rb == 0) {
			return;
		}

		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;

		long end = Math.min(wb, bufs.length - 1);
		for (long num = 0, i = rb; i <= end; i++, num++) {
			BLOCK tmp = bufs[(int) num];
			blockFactory.returnBlock(tmp);

			setBlock((int) num, bufs[(int) i]);
			setBlock((int) i, null);
		}

		long distance = wi - ri;
		limitpos = ((limitpos >>> blocksizesqrt) - rb) << blocksizesqrt;
		ri = ri & blocksizemask;
		wi = ri + distance;
		//		System.out.println("compact...");
	}

	public void wbyte(int data) {
		checkWrite(1);
		setByte(wi, data);
		wi++;
	}

	public byte rbyte() {
		checkRead(1);
		byte ret = getByte(ri);
		ri++;
		return ret;
	}

	public void sbyte(long wi, int data) {
		checkSet(wi, 1);
		setByte(wi, data);
	}

	public byte gbyte(long ri) {
		checkGet(ri, 1);
		byte ret = getByte(ri);
		return ret;
	}

	public void wshort(short data) {
		int dlen = 2;
		checkWrite(dlen);
		setShort(dlen, wi, data);
		wi += dlen;
	}

	public short rshort() {
		int dlen = 2;
		checkRead(dlen);
		short ret = getShort(dlen, ri);
		ri += dlen;
		return ret;
	}

	public void sshort(long wi, short data) {
		int dlen = 2;
		checkSet(wi, dlen);
		setShort(dlen, wi, data);
	}

	public short gshort(long ri) {
		int dlen = 2;
		checkGet(ri, dlen);
		short ret = getShort(dlen, ri);
		return ret;
	}

	public void wint(int data) {
		int dlen = 4;
		checkWrite(dlen);
		setInt(dlen, wi, data);
		wi += dlen;
	}

	public int rint() {
		int dlen = 4;
		checkRead(dlen);
		int ret = getInt(dlen, ri);
		ri += dlen;
		return ret;
	}

	public void sint(long wi, int data) {
		int dlen = 4;
		checkSet(wi, dlen);
		setInt(dlen, wi, data);
	}

	public int gint(long ri) {
		int dlen = 4;
		checkGet(ri, dlen);
		int ret = getInt(dlen, ri);
		return ret;
	}

	public void wfloat(float data) {
		int dlen = 4;
		checkWrite(dlen);
		setFloat(dlen, wi, data);
		wi += dlen;
	}

	public float rfloat() {
		int dlen = 4;
		checkRead(dlen);
		float ret = getFloat(dlen, ri);
		ri += dlen;
		return ret;
	}

	public void sfloat(long wi, float data) {
		int dlen = 4;
		checkSet(wi, dlen);
		setFloat(dlen, wi, data);
	}

	public float gfloat(long ri) {
		int dlen = 4;
		checkGet(ri, dlen);
		float ret = getFloat(dlen, ri);
		return ret;
	}

	public void wlong(long data) {
		int dlen = 8;
		checkWrite(dlen);
		setLong(dlen, wi, data);
		wi += dlen;
	}

	public long rlong() {
		int dlen = 8;
		checkRead(dlen);
		long ret = getLong(dlen, ri);
		ri += dlen;
		return ret;
	}

	public void slong(long wi, long data) {
		int dlen = 8;
		checkSet(wi, dlen);
		setLong(dlen, wi, data);
	}

	public long glong(long ri) {
		int dlen = 8;
		checkGet(ri, dlen);
		long ret = getLong(dlen, ri);
		return ret;
	}

	public void wdouble(double data) {
		int dlen = 8;
		checkWrite(dlen);
		setDouble(dlen, wi, data);
		wi += dlen;
	}

	public double rdouble() {
		int dlen = 8;
		checkRead(dlen);
		double ret = getDouble(dlen, ri);
		ri += dlen;
		return ret;
	}

	public void sdouble(long wi, double data) {
		int dlen = 8;
		checkSet(wi, dlen);
		setDouble(dlen, wi, data);
	}

	public double gdouble(long ri) {
		int dlen = 8;
		checkGet(ri, dlen);
		double ret = getDouble(dlen, ri);
		return ret;
	}

	public int wstr(String str,Charset cs) {
		byte[] bytes = str.getBytes(cs);
		int bytelen = bytes.length;
		checkWrite(bytelen);
		setBytes(bytelen, wi, bytes, 0);
		wi += bytelen;
		return bytelen;
	}

	public String rstr(int len,Charset cs) {
		checkRead(len);
		byte[] bytes = new byte[len];
		getBytes(len, ri, bytes, 0);
		String ret = new String(bytes,cs);
		ri += len;
		return ret;
	}

	public void sstr(long wi, String data,Charset cs) {
		byte[] bytes = data.getBytes(cs);
		int bytelen = bytes.length;
		checkSet(wi, bytelen);
		setBytes(bytelen, wi, bytes, 0);
	}

	public String gstr(long ri, int len,Charset cs) {
		checkGet(ri, len);
		byte[] bytes = new byte[len];
		getBytes(len, ri, bytes, 0);
		String ret = new String(bytes,cs);
		return ret;
	}

	public void wbytes(byte[] data, int offset, int len) {
		checkWrite(len);
		setBytes(len, wi, data, offset);
		wi += len;
	}

	public void rbytes(byte[] buf, int offset, int len) {
		checkRead(len);
		getBytes(len, ri, buf, offset);
		ri += len;
	}

	public void sbytes(long wi, byte[] data, int offset, int len) {
		checkSet(wi, len);
		setBytes(len, wi, data, offset);
	}

	public void gbytes(long ri, byte[] buf, int offset, int len) {
		checkGet(ri, len);
		getBytes(len, ri, buf, offset);
	}

	protected void checkSet(long wi, long len) {
		if (this.wi - wi < len) {
			throw new IndexOutOfBoundsException();
		}
	}

	protected void checkGet(long ri, long len) {
		if (wi - ri < len) {
			throw new IndexOutOfBoundsException();
		}
	}

	protected void checkRead(long n) {
		if (wi - ri < n) {
			throw new IndexOutOfBoundsException();
		}
	}

	protected void checkWrite(long n) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long lastb = limitpos >>> blocksizesqrt;
		while (limitpos - wi < n) {
			while (wb >= bufs.length) {
				reallocateBufArray();
			}

			if (bufs[(int) wb] == null) {
				BLOCK buf = blockFactory.createBlock();
				setBlock((int) wb, buf);
				limitpos += blocksize;
			} else {
				setBlock((int) wb, bufs[(int) wb]);
				if (wb > lastb) {
					limitpos += blocksize;
				}
			}
			lastb = limitpos >>> blocksizesqrt;
			wb++;
		}
	}

	protected abstract BLOCK[] createBufArray(int len);

	protected abstract void setBlock(int i, BLOCK block);

	/**
	 * wrap a block to ByteBuffer to do IO operations.
	 * 
	 * @param b
	 * @param position
	 * @param limit
	 * @return
	 */
	protected abstract ByteBuffer wrapBlock(long b, long position, long limit);

	protected abstract void reallocateBufArray();

	protected abstract void copyMemoryToSingleBlock(Object obj, long address,
			long b, long bi, long num);

	protected abstract void copyMemoryFromSingleBlock(long b, long bi,
			Object obj, long address, long num);

	protected void setByte(long wi, int data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setByte_(wb, wbi, data);
	}

	protected byte getByte(long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		byte ret = getByte_(rb, rbi);
		return ret;
	}

	protected void setShort(long dlen, long wi, short data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setShort_(dlen, wb, wbi, data);
	}

	protected short getShort(long dlen, long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		short ret = getShort_(dlen, rb, rbi);
		return ret;
	}

	protected void setInt(long dlen, long wi, int data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setInt_(dlen, wb, wbi, data);
	}

	protected int getInt(int dlen, long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		int ret = getInt_(dlen, rb, rbi);
		return ret;
	}

	protected static final int[] bigEndianIntShifts = { 24, 16, 8, 0 };
	protected static final int[] littleEndianIntShifts = { 0, 8, 16, 24 };

	protected void setFloat(int dlen, long wi, float data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setFloat_(dlen, wb, wbi, data);
	}

	protected float getFloat(int dlen, long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		float ret = getFloat_(dlen, rb, rbi);
		return ret;
	}

	protected void setLong(int dlen, long wi, long data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setLong_(dlen, wb, wbi, data);
	}

	protected long getLong(int dlen, long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		long ret = getLong_(dlen, rb, rbi);
		return ret;
	}

	protected static final int[] bigEndianLongShifts = { 56, 48, 40, 32, 24,
			16, 8, 0 };
	protected static final int[] littleEndianLongShifts = { 0, 8, 16, 24, 32,
			40, 48, 56 };

	protected void setDouble(int dlen, long wi, double data) {
		long wi_ = wi;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		setDouble_(dlen, wb, wbi, data);
	}

	protected double getDouble(int dlen, long ri) {
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		double ret = getDouble_(dlen, rb, rbi);
		return ret;
	}

	private String getUtf(int bytelen, long ri,Charset cs) {
		int strlen = bytelen >>> 1;

		String ret = new String();
		char[] charbuf = new char[strlen];

		int curbytes = 0;
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		long left;
		long remains;
		while (curbytes < bytelen) {
			left = blocksize - rbi;
			remains = bytelen - curbytes;
			if (left > remains) {
				copyMemoryFromSingleBlock(rb, rbi, charbuf,
						unsafe.ARRAY_CHAR_BASE_OFFSET + curbytes, remains);
				curbytes += remains;
			} else {
				copyMemoryFromSingleBlock(rb, rbi, charbuf,
						unsafe.ARRAY_CHAR_BASE_OFFSET + curbytes, left);
				curbytes += left;
				rb++;
				rbi = 0;
			}
		}

		unsafe.putObject(ret, stringValueOffset, charbuf);
		return ret;
	}

	protected abstract void setByte_(long wb, long wbi, int data);

	protected abstract byte getByte_(long rb, long rbi);

	/**
	 * set short in single block;
	 * 
	 * @param wb
	 * @param wbi
	 * @param data
	 */
	protected abstract void setShortToSingleBlock(long wb, long wbi, short data);

	/**
	 * set short in multiblocks.
	 * 
	 * @param dlen
	 * @param wb
	 * @param wbi
	 * @param data
	 */
	protected void setShort_(long dlen, long wb, long wbi, short data) {
		if (blocksize - wbi >= dlen) {
			setShortToSingleBlock(wb, wbi, data);
		} else {
			setSplitedShort_(dlen, wb, wbi, data);
		}
	}

	private void setSplitedShort_(long dlen, long wb, long wbi, short data) {
		if (isBigEndian) {
			setByte_(wb, wbi, (byte) (data >>> 8));
			wb++;
			wbi = 0;
			setByte_(wb, wbi, (byte) data);
		} else {
			setByte_(wb, wbi, (byte) (data));
			wb++;
			wbi = 0;
			setByte_(wb, wbi, (byte) (data >>> 8));
		}

	}

	/**
	 * get short from single block
	 * 
	 * @param rb
	 * @param rbi
	 * @return
	 */
	protected abstract short getShortFromSingleBlock(long rb, long rbi);

	/**
	 * get short from multiblocks
	 * 
	 * @param dlen
	 * @param rb
	 * @param rbi
	 * @return
	 */
	protected short getShort_(long dlen, long rb, long rbi) {
		if (blocksize - rbi >= dlen) {
			return getShortFromSingleBlock(rb, rbi);
		} else {
			return getSplitedShort_(dlen, rb, rbi);
		}
	}

	private short getSplitedShort_(long dlen, long rb, long rbi) {
		if (isBigEndian) {
			short ret = (short) ((getByte_(rb, rbi) & 0xFF) << 8);
			rb++;
			rbi = 0;
			ret |= (getByte_(rb, rbi) & 0xFF);
			return ret;
		} else {
			short ret = (short) ((getByte_(rb, rbi) & 0xFF));
			rb++;
			rbi = 0;
			ret |= ((getByte_(rb, rbi) & 0xFF) << 8);
			return ret;
		}
	}

	protected abstract void setIntToSingleBlock(long wb, long wbi, int data);

	protected abstract int getIntFromSingleBlock(long rb, long rbi);

	protected void setInt_(long dlen, long wb, long wbi, int data) {

		long curLeft = blocksize - wbi;
		if (curLeft >= dlen) {
			setIntToSingleBlock(wb, wbi, data);
		} else {
			int[] arr = null;
			if (isBigEndian) {
				arr = bigEndianIntShifts;
			} else {
				arr = littleEndianIntShifts;
			}
			for (int i = 0; i < dlen; i++) {
				setByte_(wb, wbi, (byte) (data >>> arr[i]));
				wbi++;
				if (wbi >= blocksize) {
					wb++;
					wbi = 0;
				}
			}
		}
	}

	protected int getInt_(int dlen, long rb, long rbi) {
		if (blocksize - rbi >= dlen) {
			return getIntFromSingleBlock(rb, rbi);
		} else {
			int[] arr = null;
			if (isBigEndian) {
				arr = bigEndianIntShifts;
			} else {
				arr = littleEndianIntShifts;
			}
			int ret = 0;
			for (int i = 0; i < dlen; i++) {
				ret |= ((getByte_(rb, rbi) & 0xFF) << arr[i]);
				rbi++;
				if (rbi >= blocksize) {
					rb++;
					rbi = 0;
				}
			}
			return ret;
		}
	}

	protected abstract void setLongToSingleBlock(long wb, long wbi, long data);

	protected abstract long getLongFromSingleBlock(long rb, long rbi);

	protected void setLong_(int dlen, long wb, long wbi, long data) {
		if (blocksize - wbi > dlen) {
			setLongToSingleBlock(wb, wbi, data);
		} else {
			int arr[];
			if (isBigEndian) {
				arr = bigEndianLongShifts;
			} else {
				arr = littleEndianLongShifts;
			}
			for (int i = 0; i < dlen; i++) {
				setByte_(wb, wbi, (byte) (data >>> arr[i]));
				wbi++;
				if (wbi >= blocksize) {
					wb++;
					wbi = 0;
				}
			}
		}
	}

	protected long getLong_(int dlen, long rb, long rbi) {
		if (blocksize - rbi > dlen) {
			return getLongFromSingleBlock(rb, rbi);
		} else {
			int arr[];
			if (isBigEndian) {
				arr = bigEndianLongShifts;
			} else {
				arr = littleEndianLongShifts;
			}
			long ret = 0;
			for (int i = 0; i < dlen; i++) {
				ret |= ((getByte_(rb, rbi) & 0xFFL) << arr[i]);
				rbi++;
				if (rbi >= blocksize) {
					rb++;
					rbi = 0;
				}
			}
			return ret;
		}
	}

	protected void setBytes(long bytelen, long wi, byte[] data, int offset) {
		long wi_ = wi;
		long remains = bytelen;

		long left = 0;
		long wb = wi_ >>> blocksizesqrt;
		long wbi = wi_ & blocksizemask;
		while (remains > 0) {
			left = blocksize - wbi;
			if (left > remains) {
				copyMemoryToSingleBlock(data, arrBaseOff + offset
						+ ((bytelen - remains)), wb, wbi, remains);
				wbi += remains;
				remains -= remains;
			} else {
				copyMemoryToSingleBlock(data, arrBaseOff + offset
						+ ((bytelen - remains)), wb, wbi, left);
				wb++;
				wbi = 0;
				remains -= left;
			}
		}
	}

	protected void getBytes(long bytelen, long ri, byte[] dest, int offset) {
		long curbytes = 0;
		long ri_ = ri;
		long rb = ri_ >>> blocksizesqrt;
		long rbi = ri_ & blocksizemask;
		long left;
		long remains;
		while (curbytes < bytelen) {
			left = blocksize - rbi;
			remains = bytelen - curbytes;
			if (left > remains) {
				copyMemoryFromSingleBlock(rb, rbi, dest, arrBaseOff + offset
						+ curbytes, remains);
				curbytes += remains;
			} else {
				copyMemoryFromSingleBlock(rb, rbi, dest, arrBaseOff + offset
						+ curbytes, left);
				curbytes += left;
				rb++;
				rbi = 0;
			}
		}
	}

	protected void setFloat_(int dlen, long wb, long wbi, float data) {
		setInt_(dlen, wb, wbi, Float.floatToIntBits(data));
	}

	protected float getFloat_(int dlen, long rb, long rbi) {
		return Float.intBitsToFloat(getInt_(dlen, rb, rbi));
	}

	protected void setDouble_(int dlen, long wb, long wbi, double data) {
		setLong_(dlen, wb, wbi, Double.doubleToLongBits(data));
	}

	protected double getDouble_(int dlen, long rb, long rbi) {
		return Double.longBitsToDouble(getLong_(dlen, rb, rbi));
	}

	public static final sun.misc.Unsafe unsafe;
	protected static final long stringValueOffset;
	protected static final long arrBaseOff;
	static {
		unsafe = UnsafeUtil.unsafe;

		long offset = 0;
		try {
			Field f = String.class.getDeclaredField("value");
			offset = unsafe.objectFieldOffset(f);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
		stringValueOffset = offset;
		arrBaseOff = unsafe.ARRAY_BYTE_BASE_OFFSET;
	}
}
