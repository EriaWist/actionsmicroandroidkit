package com.actionsmicro.mp4;

import java.nio.ByteBuffer;
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
import com.actionsmicro.mp4.box.MediaHeaderBox;
import com.actionsmicro.mp4.box.MediaInformationBox;
import com.actionsmicro.mp4.box.MovieBox;
import com.actionsmicro.mp4.box.MovieExtendsBox;
import com.actionsmicro.mp4.box.MovieExtendsHeaderBox;
import com.actionsmicro.mp4.box.MovieHeaderBox;
import com.actionsmicro.mp4.box.SampleDescriptionBox;
import com.actionsmicro.mp4.box.SampleSizeBox;
import com.actionsmicro.mp4.box.SampleTableBox;
import com.actionsmicro.mp4.box.SampleToChunkBox;
import com.actionsmicro.mp4.box.TimeToSampleBox;
import com.actionsmicro.mp4.box.TrackBox;
import com.actionsmicro.mp4.box.TrackExtendsBox;
import com.actionsmicro.mp4.box.TrackHeaderBox;
import com.actionsmicro.mp4.box.VideoMediaHeaderBox;

public class FragmentedMP4Serializer {
	private static long REFERENCE_TIME;
	static {
		final GregorianCalendar referenceDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		referenceDate.clear();
		referenceDate.set(1904, Calendar.JANUARY, 1);
		REFERENCE_TIME = referenceDate.getTimeInMillis();
	}
	public void buildMovieHeader(ByteBuffer byteBuffer, int width, int height, 
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		FileTypeBox ftyp = new FileTypeBox(Box.FourCharCode("mp42"), 1, null);
		
		MovieBox moov = buildMoov(width, height,
				avcProfileIndication, profileCompatibility, avcLevelIndication, sps, ps);		
		
		ftyp.write(byteBuffer);
		moov.write(byteBuffer);
	}
	private MovieBox buildMoov(int width, int height, 
			byte avcProfileIndication, byte profileCompatibility, byte avcLevelIndication, byte[] sps, byte[] ps) {
		MovieBox moov = new MovieBox();
		int now = (int)((new GregorianCalendar().getTimeInMillis() - REFERENCE_TIME) * 1000);
		moov.addChild(new MovieHeaderBox(0, now, now, 2));
		
		MovieExtendsBox mvex = new MovieExtendsBox();
		moov.addChild(mvex);
		mvex.addChild(new MovieExtendsHeaderBox(0));
		mvex.addChild(new TrackExtendsBox(1, 1, 0, 0, 0));
		
		TrackBox trak = new TrackBox();
		moov.addChild(trak);
		trak.addChild(new TrackHeaderBox(0x00000f, now, now, 1, 0, width, height));
		MediaBox mdia = new MediaBox();
		trak.addChild(mdia);
		mdia.addChild(new MediaHeaderBox(now, now, 0x0000BB80, 0));
		mdia.addChild(new HandlerBox(Box.FourCharCode("vide"), "VideoHandler "));
		
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
}
