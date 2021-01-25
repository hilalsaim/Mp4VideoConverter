package model;

import java.io.File;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Video {
	private final StringProperty name;
	private final StringProperty path;
	private final StringProperty size;
	private final StringProperty resolution;
	private final File file;
	
	
	public Video(String nome, String path, String size, String resolution, File file) {
		this.name = new SimpleStringProperty(nome);
		this.path = new SimpleStringProperty(path);
		this.size = new SimpleStringProperty(size);
		this.resolution = new SimpleStringProperty(resolution);
		this.file = file;
	}

	public StringProperty getName() {
		return name;
	}

	public StringProperty getPath() {
		return path;
	}

	public StringProperty getSize() {
		return size;
	}

	public StringProperty getResolution() {
		return resolution;
	}

	public File getFile() {
		return file;
	}
	
	
	

}
