package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import android.util.SparseIntArray;

import com.actionsmicro.utils.Log;

public class SimpleMpegTsPacketizer {
	public interface PacketReceiver {

		void onPacketReady(byte[] array);
		
	}
	private static final int ADAPTATION_FIELD_EXIST_INDICATOR = 0x20;
	private static final int PAYLOAD_EXIST_INDICATOR = 0x10;
	private static final int PAYLOAD_COUNTER_MASK = 0x0f;
	private static final int PACKET_IDENTIFIER_MASK = 0x1fff00;
	private static final int PLAYLOAD_UNIT_START_INDICATOR = 0x400000;
	private static final int DEFAULT_POINTER_SIZE = 1;
	private static final int TS_HEADER_SIZE = 4;
	private static final int PACKET_SIZE = 188;
	private static final int SYNC_BYTE = 0x47000000;
	private ByteBuffer packetBuffer = ByteBuffer.allocate(PACKET_SIZE);
	private int frameCounter = 0;
	private PacketReceiver packetReceiver;
	public SimpleMpegTsPacketizer(PacketReceiver packetReceiver) throws IOException {
		this.packetReceiver = packetReceiver;
	}

	private SparseIntArray payloadCounter = new SparseIntArray();
	private int getPayloadCounter(int pid) {
		return payloadCounter.get(pid);
	}
	private void increasePayloadCounter(int pid) {
		payloadCounter.put(pid, payloadCounter.get(pid)+1);
	}
	private int writePayload(int pid, byte[] payload, int offset, int size, boolean isStartOfPayload, boolean isTableData, long presentationTimestamp) throws IOException {
		packetBuffer.clear();
		packetBuffer.order(ByteOrder.BIG_ENDIAN);
		int tsHeader = SYNC_BYTE | ((pid << 8) & PACKET_IDENTIFIER_MASK) | PAYLOAD_EXIST_INDICATOR | (getPayloadCounter(pid) & PAYLOAD_COUNTER_MASK);
		int avaiableSpaceInPacket = PACKET_SIZE - TS_HEADER_SIZE;
		if (isStartOfPayload) {
			tsHeader |= PLAYLOAD_UNIT_START_INDICATOR;
			if (isTableData) {
				avaiableSpaceInPacket -= DEFAULT_POINTER_SIZE;
			} 
			else {
				avaiableSpaceInPacket -= 8;				
			}
		}
		size = Math.min(size, avaiableSpaceInPacket);
		int adaptationFieldSize = avaiableSpaceInPacket - size;
		if (isStartOfPayload && !isTableData) {
			adaptationFieldSize = 8;
			tsHeader |= ADAPTATION_FIELD_EXIST_INDICATOR;
		} else {
			if (adaptationFieldSize > 0) {
				tsHeader = tsHeader | ADAPTATION_FIELD_EXIST_INDICATOR;
			}
		}
		packetBuffer.putInt(tsHeader);
		if (isStartOfPayload && !isTableData) {
			long adaptationField = 0x0710000000000000L;
			long pcr = (long) ((double)(System.currentTimeMillis()) * (90000.0/1000.0));//((double)(presentationTimestamp) * (90000.0/1000000.0));//((double)(System.currentTimeMillis()) * (90000.0/1000.0));
			adaptationField = (adaptationField) | ((pcr << 15) & 0x0000FFFFFFFFFE00L);
			packetBuffer.putLong(adaptationField);
		} else {
			if (adaptationFieldSize > 0) {
				putAdaptationFields(adaptationFieldSize);
			}
		}
		if ((PLAYLOAD_UNIT_START_INDICATOR & tsHeader) != 0 && isTableData) {
			packetBuffer.put((byte)0x00);//pointer byte needed when PLAYLOAD_UNIT_START_INDICATOR is set. (isStartOfPayload is true)
		}
		try {
			packetBuffer.put(payload, offset, size);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		}
		if (packetReceiver != null) {
			packetReceiver.onPacketReady(packetBuffer.array());
		}
		increasePayloadCounter(pid);
		return size;
	}
	private void writePayload(int pid, byte[] payload, int offset, int size, boolean isTableData, long presentationTimestamp) throws IOException {
		int remaining = size;
		while (remaining > 0) {
			int written = writePayload(pid, payload, offset, remaining, remaining == size, isTableData, presentationTimestamp);
			offset += written;
			remaining -= written;
		}
	}

	private void putAdaptationFields(int adaptationFieldSize) {
		packetBuffer.put((byte)(adaptationFieldSize - 1));
		if (adaptationFieldSize >= 2) {
			packetBuffer.put((byte)0x00);
			int i = 2;
			while (i < adaptationFieldSize) {
				packetBuffer.put((byte)0xff);
				i++;
			}
		}
	}
	private static final int [] CRC32_TABLE = {
	    0x00000000, 0x04c11db7, 0x09823b6e, 0x0d4326d9,
	    0x130476dc, 0x17c56b6b, 0x1a864db2, 0x1e475005,
	    0x2608edb8, 0x22c9f00f, 0x2f8ad6d6, 0x2b4bcb61,
	    0x350c9b64, 0x31cd86d3, 0x3c8ea00a, 0x384fbdbd,
	    0x4c11db70, 0x48d0c6c7, 0x4593e01e, 0x4152fda9,
	    0x5f15adac, 0x5bd4b01b, 0x569796c2, 0x52568b75,
	    0x6a1936c8, 0x6ed82b7f, 0x639b0da6, 0x675a1011,
	    0x791d4014, 0x7ddc5da3, 0x709f7b7a, 0x745e66cd,
	    0x9823b6e0, 0x9ce2ab57, 0x91a18d8e, 0x95609039,
	    0x8b27c03c, 0x8fe6dd8b, 0x82a5fb52, 0x8664e6e5,
	    0xbe2b5b58, 0xbaea46ef, 0xb7a96036, 0xb3687d81,
	    0xad2f2d84, 0xa9ee3033, 0xa4ad16ea, 0xa06c0b5d,
	    0xd4326d90, 0xd0f37027, 0xddb056fe, 0xd9714b49,
	    0xc7361b4c, 0xc3f706fb, 0xceb42022, 0xca753d95,
	    0xf23a8028, 0xf6fb9d9f, 0xfbb8bb46, 0xff79a6f1,
	    0xe13ef6f4, 0xe5ffeb43, 0xe8bccd9a, 0xec7dd02d,
	    0x34867077, 0x30476dc0, 0x3d044b19, 0x39c556ae,
	    0x278206ab, 0x23431b1c, 0x2e003dc5, 0x2ac12072,
	    0x128e9dcf, 0x164f8078, 0x1b0ca6a1, 0x1fcdbb16,
	    0x018aeb13, 0x054bf6a4, 0x0808d07d, 0x0cc9cdca,
	    0x7897ab07, 0x7c56b6b0, 0x71159069, 0x75d48dde,
	    0x6b93dddb, 0x6f52c06c, 0x6211e6b5, 0x66d0fb02,
	    0x5e9f46bf, 0x5a5e5b08, 0x571d7dd1, 0x53dc6066,
	    0x4d9b3063, 0x495a2dd4, 0x44190b0d, 0x40d816ba,
	    0xaca5c697, 0xa864db20, 0xa527fdf9, 0xa1e6e04e,
	    0xbfa1b04b, 0xbb60adfc, 0xb6238b25, 0xb2e29692,
	    0x8aad2b2f, 0x8e6c3698, 0x832f1041, 0x87ee0df6,
	    0x99a95df3, 0x9d684044, 0x902b669d, 0x94ea7b2a,
	    0xe0b41de7, 0xe4750050, 0xe9362689, 0xedf73b3e,
	    0xf3b06b3b, 0xf771768c, 0xfa325055, 0xfef34de2,
	    0xc6bcf05f, 0xc27dede8, 0xcf3ecb31, 0xcbffd686,
	    0xd5b88683, 0xd1799b34, 0xdc3abded, 0xd8fba05a,
	    0x690ce0ee, 0x6dcdfd59, 0x608edb80, 0x644fc637,
	    0x7a089632, 0x7ec98b85, 0x738aad5c, 0x774bb0eb,
	    0x4f040d56, 0x4bc510e1, 0x46863638, 0x42472b8f,
	    0x5c007b8a, 0x58c1663d, 0x558240e4, 0x51435d53,
	    0x251d3b9e, 0x21dc2629, 0x2c9f00f0, 0x285e1d47,
	    0x36194d42, 0x32d850f5, 0x3f9b762c, 0x3b5a6b9b,
	    0x0315d626, 0x07d4cb91, 0x0a97ed48, 0x0e56f0ff,
	    0x1011a0fa, 0x14d0bd4d, 0x19939b94, 0x1d528623,
	    0xf12f560e, 0xf5ee4bb9, 0xf8ad6d60, 0xfc6c70d7,
	    0xe22b20d2, 0xe6ea3d65, 0xeba91bbc, 0xef68060b,
	    0xd727bbb6, 0xd3e6a601, 0xdea580d8, 0xda649d6f,
	    0xc423cd6a, 0xc0e2d0dd, 0xcda1f604, 0xc960ebb3,
	    0xbd3e8d7e, 0xb9ff90c9, 0xb4bcb610, 0xb07daba7,
	    0xae3afba2, 0xaafbe615, 0xa7b8c0cc, 0xa379dd7b,
	    0x9b3660c6, 0x9ff77d71, 0x92b45ba8, 0x9675461f,
	    0x8832161a, 0x8cf30bad, 0x81b02d74, 0x857130c3,
	    0x5d8a9099, 0x594b8d2e, 0x5408abf7, 0x50c9b640,
	    0x4e8ee645, 0x4a4ffbf2, 0x470cdd2b, 0x43cdc09c,
	    0x7b827d21, 0x7f436096, 0x7200464f, 0x76c15bf8,
	    0x68860bfd, 0x6c47164a, 0x61043093, 0x65c52d24,
	    0x119b4be9, 0x155a565e, 0x18197087, 0x1cd86d30,
	    0x029f3d35, 0x065e2082, 0x0b1d065b, 0x0fdc1bec,
	    0x3793a651, 0x3352bbe6, 0x3e119d3f, 0x3ad08088,
	    0x2497d08d, 0x2056cd3a, 0x2d15ebe3, 0x29d4f654,
	    0xc5a92679, 0xc1683bce, 0xcc2b1d17, 0xc8ea00a0,
	    0xd6ad50a5, 0xd26c4d12, 0xdf2f6bcb, 0xdbee767c,
	    0xe3a1cbc1, 0xe760d676, 0xea23f0af, 0xeee2ed18,
	    0xf0a5bd1d, 0xf464a0aa, 0xf9278673, 0xfde69bc4,
	    0x89b8fd09, 0x8d79e0be, 0x803ac667, 0x84fbdbd0,
	    0x9abc8bd5, 0x9e7d9662, 0x933eb0bb, 0x97ffad0c,
	    0xafb010b1, 0xab710d06, 0xa6322bdf, 0xa2f33668,
	    0xbcb4666d, 0xb8757bda, 0xb5365d03, 0xb1f740b4
	};
	private int crcCheckSum(byte[] input, int offset, int length) {
		int crc = 0xffffffff;
	    for(int i = offset; i < offset+length; i++) {
	        crc = (crc << 8) ^ CRC32_TABLE[((crc >> 24) ^ (input[i] & 0xff)) & 0xff];
	    }
	    return crc;
	}
	private void writePat() throws IOException {
		byte[] pat = {(byte)0x00, (byte)0xB0, (byte)0x0D, (byte)0x00, (byte)0x01, (byte)0xC1, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xEF, (byte)0xFF, (byte)0x36, (byte)0x90, (byte)0xE2, (byte)0x3D};
		// hard coded PAT
		CRC32 crc32 = new CRC32();
		crc32.update(pat, 0, pat.length - 4);
		long crc32CheckSum = crc32.getValue();
		Log.d("TS", "PAT Check sum:"+crc32CheckSum+", "+Integer.toHexString(crcCheckSum(pat, 0, pat.length-4)));

		int pid = 0x000;
		writePayload(pid, pat, 0 , pat.length, true, 0);		
	}
	private void writePmt() throws IOException {
//		byte[] pmt = {(byte)0x02, (byte)0xB0, (byte)0x12, (byte)0x00, (byte)0x01, (byte)0xC3, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xF0, (byte)0x00, (byte)0x1B, (byte)0xE1, (byte)0x00, (byte)0xF0, (byte)0x00, (byte)0xCE, (byte)0xB6, (byte)0x87, (byte)0xEC};
		byte[] pmt = {(byte)0x02, (byte)0xB0, (byte)0x12, (byte)0x00, (byte)0x01, (byte)0xC3, (byte)0x00, (byte)0x00, (byte)0xE1, (byte)0x00, (byte)0xF0, (byte)0x00, (byte)0x1B, (byte)0xE1, (byte)0x00, (byte)0xF0, (byte)0x00, (byte)0xCE, (byte)0xB6, (byte)0x87, (byte)0xEC};
		// hard coded PMT
		CRC32 crc32 = new CRC32();
		crc32.update(pmt, 0, pmt.length - 4);
		long crc32CheckSum = crc32.getValue();
		Log.d("TS", "PMT Check sum:"+crc32CheckSum+", "+Integer.toHexString(crcCheckSum(pmt, 0, pmt.length-4)));
		ByteBuffer pmtBuffer = ByteBuffer.allocate(pmt.length);
		pmtBuffer.put(pmt, 0, pmt.length - 4);
		pmtBuffer.putInt(crcCheckSum(pmt, 0, pmt.length - 4));
		int pid = 0xfff;
		writePayload(pid, pmtBuffer.array(), 0 , pmtBuffer.array().length, true, 0);
	}
	private long convertToPts(long timeStamp) {
		timeStamp = (long) ((double)timeStamp*(90000.0/1000000.0));
		return 0x3100010001L | 
				((timeStamp & 0x1C0000000L) << 3) | 
				((timeStamp & 0x3FFF8000L) << 2) |
				((timeStamp & 0x7FFFL) << 1);
	}
	private long convertToDts(long timeStamp) {
		timeStamp = (long) ((double)timeStamp*(90000.0/1000000.0));
			return 0x1100010001L | 
				((timeStamp & 0x1C0000000L) << 3) | 
				((timeStamp & 0x3FFF8000L) << 2) |
				((timeStamp & 0x7FFFL) << 1);
	}
	public void writeFrame(byte[] h264Frame, int offset, int length, long presentationTimestamp) throws IOException {
		if (frameCounter % 5 == 0) {
			writePat();
			writePmt();
		}
		frameCounter ++;
		Log.d("TS", "writeFrame: length:"+length);
		Log.d("TS", "presentationTimestamp:"+presentationTimestamp);
		ByteBuffer pes = ByteBuffer.allocate(length+19+6);
		pes.order(ByteOrder.BIG_ENDIAN);
//		byte[] pesHeader = {(byte)0x00, (byte)0x00, (byte)0x01, (byte)0xE0, (byte)0x0D, (byte)0x0C, (byte)0x84, (byte)0xC0, (byte)0x0A};
		pes.put((byte)0x00);
		pes.put((byte)0x00);
		pes.put((byte)0x01);
		pes.put((byte)0xE0);
		if (length+3+10+6 > 0xFFFF) {
			Log.e("TS", "packet length overflow");
		}
		pes.putShort((short) (length+3+10+6));
		pes.put((byte)0x80);
		pes.put((byte)0xC0);
		pes.put((byte)0x0A);
		long pts = convertToPts(presentationTimestamp);
		put5LowerBytes(pes, pts);
		long dts = convertToDts(presentationTimestamp);
		put5LowerBytes(pes, dts);
		pes.putInt(0x00000001);
		pes.putShort((short)0x09E0);
		pes.put(h264Frame, offset, length);
		writePayload(0x100, pes.array(), 0, pes.limit(), false, presentationTimestamp);
	}
	private void put5LowerBytes(ByteBuffer pes, long timeStampe) {
		pes.put((byte)((timeStampe&0xFF00000000L)>>(8*4)));
		pes.put((byte)((timeStampe&0xFF000000L)>>(8*3)));
		pes.put((byte)((timeStampe&0xFF0000L)>>(8*2)));
		pes.put((byte)((timeStampe&0xFF00L)>>(8)));
		pes.put((byte)((timeStampe&0xFFL)));
	}
	public PacketReceiver getPacketReceiver() {
		return packetReceiver;
	}
	public void setPacketReceiver(PacketReceiver packetReceiver) {
		this.packetReceiver = packetReceiver;
	}
}
