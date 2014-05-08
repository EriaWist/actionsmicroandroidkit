package vavi.apps.shairport;

public interface AudioPlayer {

	void flush();

	void setVolume(double d);

	void stop();

	int getServerPort();

}
