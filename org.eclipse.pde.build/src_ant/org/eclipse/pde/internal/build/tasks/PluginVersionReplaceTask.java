/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/** 
 * This task aims at replacing the generic ids used into a plugin.xml by another value.
 */
public class PluginVersionReplaceTask extends Task {
	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String FRAGMENT = "fragment"; //$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String BACKSLASH = "\""; //$NON-NLS-1$

	//Path of the file where we are replacing the values
	private String pluginFilePath;
	private boolean plugin = true;
	private String newVersion;

	/**
	 * The location of a feature.xml file 
	 * @param path
	 */
	public void setPluginFilePath(String path) {
		pluginFilePath = path;
	}

	public void setVersionNumber(String qualifier) {
		newVersion = qualifier;
	}

	public void execute() {
		StringBuffer buffer = null;
		try {
			buffer = readFile(new File(pluginFilePath));
		} catch (IOException e) {
			throw new BuildException(e);
		}

		//Find the word plugin or fragment
		int startPlugin;
		if (plugin)
			startPlugin = scan(buffer, 0, PLUGIN);
		else
			startPlugin = scan(buffer, 0, FRAGMENT);

		int endPlugin = scan(buffer, startPlugin + 1, ">"); //$NON-NLS-1$

		//Find the version tag in the plugin header
		int versionAttr = scan(buffer, startPlugin, VERSION);
		if (versionAttr == -1 || versionAttr > endPlugin)
			return;

		//Extract the version id and replace it
		int startVersionId = scan(buffer, versionAttr + 1, BACKSLASH);
		int endVersionId = scan(buffer, startVersionId + 1, BACKSLASH);

		startVersionId++;
		buffer.replace(startVersionId, endVersionId, newVersion);

		try {
			transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(pluginFilePath));
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	private int scan(StringBuffer buf, int start, String targetName) {
		return scan(buf, start, new String[] {targetName});
	}

	private int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equals(match))
						return i;
				}
			}
		}
		return -1;
	}

	private StringBuffer readFile(File targetName) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(targetName)));
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}

	private static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}