package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;

public class TempFileManager {
	private static final int TEMP_FILE_LIMIT = 10;
	private Hashtable clients = new Hashtable();

	public TempFileManager() {
	}

	public File createTempFile(Object client, String prefix, String suffix)
		throws CoreException {
		ArrayList files = (ArrayList) clients.get(client);
		if (files == null) {
			files = new ArrayList();
			clients.put(client, files);
		}
		try {
			File tmpFile = File.createTempFile(prefix, suffix);
			if (files.size() > TEMP_FILE_LIMIT)
				purgeTempFiles(files);
			files.add(tmpFile);
			return tmpFile;
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDECore.PLUGIN_ID,
					IStatus.OK,
					null,
					e);
			throw new CoreException(status);
		}
	}

	private void purgeTempFiles(ArrayList tempFiles) {
		File[] files = (File[]) tempFiles.toArray(new File[tempFiles.size()]);
		for (int i = 0; i < files.length; i++) {
			File tempFile = files[i];
			if (tempFile.delete())
				tempFiles.remove(tempFile);
		}
	}

	public void disconnect(Object client) {
		ArrayList files = (ArrayList) clients.get(client);
		if (files != null) {
			purgeTempFiles(files);
			clients.remove(client);
		}
	}

	public void shutdown() {
		for (Enumeration enum = clients.elements(); enum.hasMoreElements();) {
			ArrayList files = (ArrayList) enum.nextElement();
			purgeTempFiles(files);
		}
		clients.clear();
	}
}