package conversor;


public interface ProgressListener  {
	
	public void progress (int percentage);
	
	public void message (String message);
	
	public void complete(String pathToFile);
	
	public void error();

}
