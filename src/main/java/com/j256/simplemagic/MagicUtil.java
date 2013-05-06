package com.j256.simplemagic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.j256.simplemagic.entries.MagicEntry;

/**
 * Class which reads in the magic files.
 * 
 * @author graywatson
 */
public class MagicUtil {

	private final static String DEFAULT_MAGIC_FILE = "/usr/share/file/magic";
	private final static int DEFAULT_READ_SIZE = 100 * 1024;

	private List<MagicEntry> magicEntries;

	/**
	 * Try to automatically find and read in the system's magic files.
	 */
	public void loadSystemMagicFiles() throws IOException {
		File file = new File(DEFAULT_MAGIC_FILE);
		if (!file.exists()) {
			throw new IOException("Default magic file/directory does not exist: " + DEFAULT_MAGIC_FILE);
		} else if (file.isFile()) {
			loadMagicFile(file);
		} else if (file.isDirectory()) {
			loadMagicFileDirectory(file);
		} else {
			throw new IOException("Default magic is not a file or a directory: " + DEFAULT_MAGIC_FILE);
		}
	}

	/**
	 * Read magic files from a directory of files.
	 */
	public void loadMagicFileDirectory(File directory) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Magic file directory is not a directory: " + directory);
		}
		List<MagicEntry> entryList = new ArrayList<MagicEntry>();
		for (File file : directory.listFiles()) {
			try {
				readFile(entryList, file);
			} catch (IOException e) {
				// ignore the file
			}
		}
		magicEntries = entryList;
	}

	/**
	 * Read magic files from a directory of files.
	 * 
	 * @return True if it worked otherwise false on an IO error.
	 */
	public void loadMagicFile(File file) throws IOException {
		List<MagicEntry> entryList = new ArrayList<MagicEntry>();
		readFile(entryList, file);
		magicEntries = entryList;
	}

	/**
	 * Return the best content type for the file or null if none found.
	 */
	public ContentType contentFromFile(String filePath) throws IOException {
		return contentFromFile(new File(filePath));
	}

	/**
	 * Return the best content type for the file or null if none found.
	 */
	public ContentType contentFromFile(File file) throws IOException {
		int readSize = DEFAULT_READ_SIZE;
		if (file.length() < readSize) {
			readSize = (int) file.length();
		}
		byte[] bytes = new byte[readSize];
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			fis.read(bytes);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				// ignored
			}
		}
		return contentFromBytes(bytes);
	}

	/**
	 * Return the best content type from the associated bytes or null if none found.
	 */
	public ContentType contentFromBytes(byte[] bytes) {
		if (magicEntries == null) {
			throw new IllegalStateException("Magic files have not been loaded");
		}
		for (MagicEntry entry : magicEntries) {
			ContentType contentType = entry.processBytes(bytes);
			if (contentType != null) {
				return contentType;
			}
		}
		return null;
	}

	private static void readFile(List<MagicEntry> entryList, File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		MagicEntry previous = null;
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				return;
			}
			if (line.length() == 0 || line.charAt(0) == '#') {
				continue;
			}

			MagicEntry entry;
			try {
				entry = MagicEntry.parseString(previous, line);
			} catch (IllegalArgumentException e) {
				// System.err.println("Error in entry: " + e);
				// ignore this entry
				continue;
			}
			if (entry == null) {
				continue;
			}
			if (entry.getLevel() == 0) {
				entryList.add(entry);
			}
			previous = entry;
		}
	}
}
