package utils;

import java.io.BufferedReader;
import java.util.concurrent.Callable;

/**
 * TaskContainer is a class that implements Callable to perform an action. Your objective
 * is to perform an action that would trap a thread, within a task to be called with a timeout
 * that way, there is no risk of the thread being frozen waiting for the readline of a stream, for example
 */
public class TaskContainer implements Callable<String>{

	BufferedReader reader;
	
	public TaskContainer(BufferedReader receivedReader) {
		this.reader = receivedReader;
	}
	
	@Override
	public String call() throws Exception {
		return reader.readLine();
	}
	
}
