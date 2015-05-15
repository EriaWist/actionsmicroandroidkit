package com.actionsmicro.mp4;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.actionsmicro.mp4.box.AvcSampleEntry;
import com.actionsmicro.mp4.box.Box;
import com.actionsmicro.mp4.box.ChunkOffsetBox;
import com.actionsmicro.mp4.box.DataEntryUrlBox;
import com.actionsmicro.mp4.box.DataInformationBox;
import com.actionsmicro.mp4.box.DataReferenceBox;
import com.actionsmicro.mp4.box.FileTypeBox;
import com.actionsmicro.mp4.box.HandlerBox;
import com.actionsmicro.mp4.box.MediaBox;
import com.actionsmicro.mp4.box.MediaDataBox;
import com.actionsmicro.mp4.box.MediaHeaderBox;
import com.actionsmicro.mp4.box.MediaInformationBox;
import com.actionsmicro.mp4.box.MovieBox;
import com.actionsmicro.mp4.box.MovieExtendsBox;
import com.actionsmicro.mp4.box.MovieExtendsHeaderBox;
import com.actionsmicro.mp4.box.MovieFragmentBox;
import com.actionsmicro.mp4.box.MovieFragmentHeaderBox;
import com.actionsmicro.mp4.box.MovieHeaderBox;
import com.actionsmicro.mp4.box.ObjectDescriptorBox;
import com.actionsmicro.mp4.box.SampleDescriptionBox;
import com.actionsmicro.mp4.box.SampleSizeBox;
import com.actionsmicro.mp4.box.SampleTableBox;
import com.actionsmicro.mp4.box.SampleToChunkBox;
import com.actionsmicro.mp4.box.TimeToSampleBox;
import com.actionsmicro.mp4.box.TrackBox;
import com.actionsmicro.mp4.box.TrackExtendsBox;
import com.actionsmicro.mp4.box.TrackFragmentBox;
import com.actionsmicro.mp4.box.TrackFragmentHeaderBox;
import com.actionsmicro.mp4.box.TrackHeaderBox;
import com.actionsmicro.mp4.box.TrackRunBox;
import com.actionsmicro.mp4.box.VideoMediaHeaderBox;

public class FragmentedMP4Serializer {
	private static long REFERENCE_TIME;
	static {
		final GregorianCalendar referenceDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		referenceDate.clear();
		referenceDate.set(1904, Calendar.JANUARY, 1);
		REFERENCE_TIME = referenceDate.getTimeInMillis();
	}
	public interface OutputListener {

		void headerReady(byte[] data, int offset, int length);
		
		void fragmentDataReady(byte[] data, int offset, int length);

		boolean shouldFinalizeMdat(MediaDataBox mdat);
		
	}
	private OutputListener outputListener;
	public OutputListener getOutputListener() {
		return outputListener;
	}
	public void setOutputListener(OutputListener outputListener) {
		this.outputListener = outputListener;
	}
	private MovieFragmentBox moof;
	private int sequenceNumber = 1;
	private TrackFragmentHeaderBox tfhd;
	private void buildMovieHeader(ByteBuffer byteBuffer, int width, int height, 
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		FileTypeBox ftyp = new FileTypeBox(Box.FourCharCode("mp42"), 1, 
				Arrays.asList(Box.FourCharCode("isom"), Box.FourCharCode("mp42"), Box.FourCharCode("dash")));
		
		MovieBox moov = buildMoov(width, height,
				avcProfileIndication, profileCompatibility, avcLevelIndication, sps, ps);		
		
		ftyp.write(byteBuffer);
		moov.write(byteBuffer);		
	}
	private MovieBox buildMoov(int width, int height, 
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		MovieBox moov = new MovieBox();
		int now = (int)((new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTimeInMillis() - REFERENCE_TIME) * 1000);
		int timescale = 0x0000BB80; // 48000
		moov.addChild(new MovieHeaderBox(0, now, now, 2, timescale));
		moov.addChild(new ObjectDescriptorBox());
		
		MovieExtendsBox mvex = new MovieExtendsBox();
		moov.addChild(mvex);
		mvex.addChild(new MovieExtendsHeaderBox(0));
		int defaultSampleDuration = timescale/24; // 2002
		mvex.addChild(new TrackExtendsBox(1, 1, defaultSampleDuration, 0, 0x00010000));
		
		TrackBox trak = new TrackBox();
		moov.addChild(trak);
		trak.addChild(new TrackHeaderBox(0x00000f, now, now, 1, 0, width, height));
		MediaBox mdia = new MediaBox();
		trak.addChild(mdia);
		mdia.addChild(new MediaHeaderBox(now, now, timescale, 0)); //0x0000BB80
		mdia.addChild(new HandlerBox(Box.FourCharCode("vide"), "VideoHandler"));
		
		MediaInformationBox minf = new MediaInformationBox();
		mdia.addChild(minf);
		minf.addChild(new VideoMediaHeaderBox());
		DataInformationBox dinf = new DataInformationBox();
		minf.addChild(dinf);
		DataReferenceBox dref = new DataReferenceBox();
		dinf.addChild(dref);
		dref.addDataEntry(new DataEntryUrlBox());
		SampleTableBox stbl = new SampleTableBox();
		minf.addChild(stbl);
		SampleDescriptionBox stsd = new SampleDescriptionBox();
		stbl.addChild(stsd);
		AvcSampleEntry avc1 = new AvcSampleEntry((short) 1, (short)width, (short)height,
				avcProfileIndication, profileCompatibility, avcLevelIndication, sps, ps);
		stsd.addSampleEntry(avc1);
		
		stbl.addChild(new TimeToSampleBox());
		stbl.addChild(new SampleToChunkBox());
		stbl.addChild(new SampleSizeBox());
		stbl.addChild(new ChunkOffsetBox());
		
		return moov;
	}
	private long currentOffset;
	private TrackRunBox trun;
	private MediaDataBox mdat;
	private TrackFragmentBox traf;
	private void finalizeFragment() {
		long baseDataOffset = currentOffset + moof.getBoxSize();
		if (tfhd != null) {
			tfhd.setBaseDataOffset(baseDataOffset+8); //8 bytes box header (length + fourcc) 
		}
		currentOffset += moof.getBoxSize() + mdat.getBoxSize();
		if (outputListener != null) {
			ByteBuffer buffer = ByteBuffer.allocate(moof.getBoxSize() + mdat.getBoxSize());
			moof.write(buffer);
			mdat.write(buffer);
			outputListener.fragmentDataReady(buffer.array(), 0, buffer.position());
		}
		moof = null;
		mdat = null;
		trun = null;
	}
	public void prepare(int width, int height, 
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		ByteBuffer buffer = ByteBuffer.allocate(2048);
		
		buildMovieHeader(buffer, width, height,
				avcProfileIndication, profileCompatibility, avcLevelIndication, sps, ps);
		
		if (outputListener != null) {
			outputListener.headerReady(buffer.array(), 0, buffer.position());
		}
		
		currentOffset = buffer.position();
	}
	private static final int FRAGMENT_SIZE = 64*1024*4;
	public void addH264Frame(byte[] h264, int offset, int len) {
		if (moof == null) {
			moof = new MovieFragmentBox();
			moof.addChild(new MovieFragmentHeaderBox(sequenceNumber));
			traf = new TrackFragmentBox();
			moof.addChild(traf);
			tfhd = new TrackFragmentHeaderBox(1);
			traf.addChild(tfhd);
			mdat = new MediaDataBox(FRAGMENT_SIZE);
			sequenceNumber++;		
		}
		byte naluType = (byte) (h264[offset] & 0x1F);
		if (naluType == 5) { // IDR
			int mdatOffset = mdat.getDataSize();
			if (mdatOffset != 0) {
				trun = new TrackRunBox(0, mdatOffset);
			} else {
				trun = new TrackRunBox(0);				
			}
			traf.addChild(trun);
		} else {
			if (trun == null) {
				trun = new TrackRunBox();
				traf.addChild(trun);
			}
		}
		trun.addSampleSize(len+4); // four bytes mp4 NALU header
		mdat.addSlice(h264, offset, len);
		
		if (outputListener != null && outputListener.shouldFinalizeMdat(mdat)) {
			finalizeFragment();
		}
//		int mdatSize = mdat.getBoxSize();
//		if (mdatSize >= (FRAGMENT_SIZE-1024)) {
////		if (mdat.getSliceCount()%3 == 0) {
//					finalizeFragment();
//		}
	}
}
