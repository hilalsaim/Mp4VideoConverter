package utils;

import java.io.File;

public class DirectoryUtils {
	
	public static void createDirIfNonExistant(String outputPath) {
		//Creating target path folder, if it does not exist
		if (!(new File(outputPath).exists())) {
			new File(outputPath).mkdirs();
		}
	}

}
