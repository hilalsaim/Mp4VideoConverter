package conversor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FilenameUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import utils.NumberUtils;
import utils.StringUtils;
import utils.TaskContainer;


public class FFMpegWrapper {

	private String pathToFFMpeg;
	private static int TOLERANCE_TO_NULLS = 10;
	boolean isComplete = false;
	ProgressListener progressListener;
	Process ffmpeg;
	InputStream ffmpegErrorStream;
	BufferedReader errorStreamReader;
	OutputStream ffmpegOutputStream;

	/**
	 * Constructor used if the ffmpeg location is not the default
	 * @param pathToFFMpeg
	 */
	public FFMpegWrapper(String pathToFFMpeg) {
		this.pathToFFMpeg = pathToFFMpeg;
	}

	public FFMpegWrapper() {
		this.pathToFFMpeg = "ff/ffmpeg.exe";
	}


	//throws IOException, InterruptedException, ExecutionException, TimeoutException
	public void converterVideo(ArrayList<String> params, ProgressListener progressListener)  {
		this.progressListener = progressListener;
		String[] parameters = StringUtils.stringListToArray(params);

		try {
			executeFFmpeg(parameters);

			initializeFFmpegStreams();

			readFFmpegStream();

			if (isComplete) {
				// Sending the last parameter, which is the absolute path to the file
				progressListener.progress(100);
				progressListener.complete(params.get(params.size() - 1)); 
			} else {
				progressListener.error();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void executeFFmpeg(String[] parameters) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		ffmpeg = runtime.exec(parameters); //CONVERSION STARTS HERE
		ProcessKiller ffmpegKiller = new ProcessKiller(ffmpeg);
		runtime.addShutdownHook(ffmpegKiller);
	}

	private void initializeFFmpegStreams() {
		ffmpegErrorStream = ffmpeg.getErrorStream();
		errorStreamReader = new BufferedReader(new InputStreamReader(ffmpegErrorStream));
		ffmpegOutputStream = ffmpeg.getOutputStream();
	}

	private void readFFmpegStream() {
		String lineContent;
		int currentNullAmount = 0;
		int currLine = 0;

		for (currLine = 0; true; currLine++) {
			try {
				System.out.println("for  "+ currLine);
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future<String> future = executor.submit(new TaskContainer(errorStreamReader));

				lineContent = future.get(3, TimeUnit.SECONDS);
				
				if (lineContent == null || lineContent.isEmpty()) {
					ffmpegOutputStream.write(new String("A").getBytes()); //Writing something for ffmpeg to unlock (solving rare case)
					currentNullAmount +=1;

					if(!ffmpeg.isAlive()) {
						isComplete = true;
						break;
					}

					if (currentNullAmount >= TOLERANCE_TO_NULLS) {
						isComplete = false;
						ffmpeg.destroy();
						break;
					}

				} else {
					currentNullAmount = 0;
					System.out.println(lineContent);
					progressListener.message(lineContent);
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
				currentNullAmount +=1;
				if (currentNullAmount >= TOLERANCE_TO_NULLS) {
					isComplete = false;
					ffmpeg.destroy();
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public HashMap<String, String> getFFmpegPropertiesAsHash(String message) {
		HashMap<String, String> properties = new HashMap<String, String>();
		if (message.contains("time")) {
			Iterable<String> results = Splitter.on(CharMatcher.anyOf("= "))
					.trimResults()
					.omitEmptyStrings()
					.split(message);

			List<String> propertiesList = Lists.newArrayList(results);

			if (propertiesList.size() % 2 == 0) {
				for(int i = 0; i < propertiesList.size(); i+= 2) {
					properties.put(propertiesList.get(i), propertiesList.get(i+1));
				}
				return properties;
			}
		}
		return null;
	}


	public ArrayList<String> construirParametrosFfmpegVideo(String filePath, String fileName, String savePath, String bitRate, 
			int width, int height) {

		ArrayList<String> params = new ArrayList<String>(); 

		File ffmpegLocation = new File(pathToFFMpeg);
		String pathToFFmpeg = ffmpegLocation.getAbsolutePath();

		params.add(pathToFFmpeg); //Path to the executable
		params.add("-i"); //Source file path
		params.add(filePath);
		params.add("-y"); //Replaces existing files of the same name

		if (!bitRate.isEmpty() && bitRate != null) {
			params.add("-maxrate");
			params.add(bitRate);
		}

		//Maintaining aspect ratio and setting video resolution

		double aspectRatio = NumberUtils.truncateDecimal((double) width / height, 1).doubleValue();

		if (aspectRatio == 1.7) { // 16:9
			params.add("-s");
			params.add("768x432");
		} else if (aspectRatio == 1.6) { // 16:10
			params.add("-s");
			params.add("768x480");
		} else { // 4:3 e qualquer outro
			params.add("-s");
			params.add("640x480");
		}

		params.add("-crf");
		params.add("23");

		params.add("-c:v");
		params.add("libx264");

		params.add(savePath + FilenameUtils.removeExtension(fileName) + " - Convert.mp4");

		return params;

	}


}
