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
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/** 
 * This task aims at replacing the generic ids used into a feature.xml 
 * by another value.
 * 
 */
public class IdReplaceTask extends Task {
	private static final String FEATURE = "feature";//$NON-NLS-1$
	private static final String ID = "id";//$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String BACKSLASH = "\""; //$NON-NLS-1$
	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String INCLUDES = "includes"; //$NON-NLS-1$

	//Path of the file where we are replacing the values
	private String featureFilePath;
	//Map of the plugin ids (key) and their version number (value)  
	private Map pluginIds = new HashMap(10);
	//Map of the feature ids (key) and their version number (value)
	private Map featureIds = new HashMap(4);

	private final static String GENERIC_VERSION_NUMBER = "0.0.0"; //$NON-NLS-1$

	/**
	 * The location of a feature.xml file 
	 * @param path
	 */
	public void setFeatureFilePath(String path) {
		featureFilePath = path;
	}

	/**
	 * Set the values to use when replacing a generic value used in a plugin reference.
	 * Note all the pluginIds that have a generic number into the feature.xml must be
	 * listed in <param>values</param>.
	 * @param values: a comma separated list alternating pluginId and versionNumber.
	 * For example: org.eclipse.pde.build,2.1.0,org.eclipse.core.resources,1.2.0
	 */
	public void setPluginIds(String values) {
		pluginIds = new HashMap(10);
		for (StringTokenizer tokens = new StringTokenizer(values, COMMA); tokens.hasMoreTokens();) { //$NON-NLS-1$
			String token = tokens.nextToken().trim();
			String id = EMPTY;
			if (!token.equals(EMPTY))
				id = token;

			String version = EMPTY;
			token = tokens.nextToken().trim();
			if (!token.equals(EMPTY))
				version = token;
			pluginIds.put(id, version);
		}
	}

	/**
	 * Set the values to use when replacing a generic value used in a feature reference
	 * Note that all the featureIds that have a generic number into the feature.xml must
	 * be liste in <param>values</param>.
	 * @param values
	 */
	public void setFeatureIds(String values) {
		featureIds = new HashMap(10);
		for (StringTokenizer tokens = new StringTokenizer(values, COMMA); tokens.hasMoreTokens();) { //$NON-NLS-1$
			String token = tokens.nextToken().trim();
			String id = EMPTY;
			if (!token.equals(EMPTY))
				id = token;

			String version = EMPTY;
			token = tokens.nextToken().trim();
			if (!token.equals(EMPTY))
				version = token;
			featureIds.put(id, version);
		}
	}

	public void execute() {
		StringBuffer buffer = null;
		try {
			buffer = readFile(new File(featureFilePath));
		} catch (IOException e) {
			throw new BuildException(e);
		}

		//Skip feature declaration because it contains the word "plugin"
		int startFeature = scan(buffer, 0, FEATURE);
		int endFeature = scan(buffer, startFeature + 1, ">"); //$NON-NLS-1$

		int startElement = endFeature;
		int startId = 0;
		while (true) {
			int startPlugin = scan(buffer, startElement + 1, PLUGIN);
			int startInclude = scan(buffer, startElement + 1, INCLUDES);

			if (startPlugin == -1 && startInclude == -1)
				break;

			int foundElement = -1;
			boolean isPlugin = false;

			//Find which a plugin or a feature is referenced first 
			if (startPlugin == -1 || startInclude == -1) {
				foundElement = startPlugin != -1 ? startPlugin : startInclude;
				isPlugin = (startPlugin != -1 ? true : false);
			} else {
				if (startPlugin < startInclude) {
					foundElement = startPlugin;
					isPlugin = true;
				} else {
					foundElement = startInclude;
					isPlugin = false;
				}
			}

			startId = scan(buffer, foundElement, ID);
			if (startId == -1)
				break;

			int startElementId = scan(buffer, startId + 1, BACKSLASH);
			int endElementId = scan(buffer, startElementId + 1, BACKSLASH);
			char[] elementId = new char[endElementId - startElementId - 1];
			buffer.getChars(startElementId + 1, endElementId, elementId, 0);

			int startVersionWord = scan(buffer, endElementId + 1, VERSION);
			int startVersionId = scan(buffer, startVersionWord + 1, BACKSLASH);
			int endVersionId = scan(buffer, startVersionId + 1, BACKSLASH);
			char[] versionId = new char[endVersionId - startVersionId - 1];
			buffer.getChars(startVersionId + 1, endVersionId, versionId, 0);
			if (!new String(versionId).equals(GENERIC_VERSION_NUMBER)) {
				startElement = startVersionId;
				continue;
			}

			startVersionId++;
			String replacementVersion = null;
			if (isPlugin) {
				replacementVersion = (String) pluginIds.get(new String(elementId));
			} else {
				replacementVersion = (String) featureIds.get(new String(elementId));
			}
			if (replacementVersion == null) {
				System.err.println("Could not find" + new String(elementId)); //$NON-NLS-1$
			} else {
				buffer.replace(startVersionId, startVersionId + GENERIC_VERSION_NUMBER.length(), replacementVersion);
			}

			startElement = startVersionId;
		}

		try {
			transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(featureFilePath));
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