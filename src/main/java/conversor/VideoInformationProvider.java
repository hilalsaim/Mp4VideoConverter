package conversor;

import java.io.File;
import java.math.BigDecimal;

/**
 * Class based on Aljava-Web methods, incorporated here to obtain information from the media
 * Extends the FFProbeWrapper class to reduce the need to create objects
 * @author marcel
 *
 */
public class VideoInformationProvider extends FFProbeWrapper{
	
	public VideoInformationProvider() throws Exception {
		super();
	}

    @Override
    public int getVideoDurationInSeconds(File media) {
        return super.getVideoDurationInSeconds(media);
    }

    public static double bytesToMB(Object sizes) {

		double size;
		if (sizes instanceof Integer) {
			int i = (int) sizes;
			size = ((double) (i / 100)) * 100;
		} else {
			size = ((Long) sizes).doubleValue();
		}

		size = size / (1024 * 1024);
		BigDecimal bd = new BigDecimal(size);
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
		double db = Double.parseDouble(bd.toPlainString());
		return db;
	}
	
	public static String bytesToMBAsString(Object sizes) {
		double size = bytesToMB(sizes);
		return Double.toString(size);
	}
	
	public static String secondsToFormattedString(int second) {
		int hours = second / 3600;
		int minutes = (second % 3600) / (60);
		int seconds = second % 60;
		return String.format("%02d:%02d:%02d",hours, minutes, seconds);
	}
}
