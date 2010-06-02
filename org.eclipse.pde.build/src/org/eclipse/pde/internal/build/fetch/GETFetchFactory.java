/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.fetch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.IAntScript;
import org.eclipse.pde.build.IFetchFactory;
import org.eclipse.pde.internal.build.*;

/**
 * This class implements a fetch factory which calls the Ant Get task on a given URL. The
 * format of the map file entry is as follows:
 * 	type@id=GET,url,[args]
 * where:
 * 	type = feature | plugin
 * 	id = plug-in or feature identifier (symbolic name)
 * 	GET = mandatory constant (to call this fetch factory)
 * 	url = url to retrieve the data from (suitable to be used in the Ant Get task)
 * 	args = optional comma-separated list of key/value pairs, specify unpack=true to indicate the element should be unzipped
 * 
 * e.g.
 * 	plugin@com.example.foobar=GET,http://downloads.example.com/com.example.foobar_1.0.jar
 * 	plugin@com.example.foobar=GET,http://downloads.example.com/com.example.foobar_1.0.jar,unpack=true
 * 	plugin@com.example.foobar=GET,http://downloads.example.com/com.example.foobar_1.0.zip,unpack=true, username=foo, password=bar
 * 
 * @since 3.2.100
 */
public class GETFetchFactory implements IFetchFactory {

	private static final String UNPACK = "unpack"; //$NON-NLS-1$
	private static final String SEPARATOR = ","; //$NON-NLS-1$
	private static final String TASK_GET = "get"; //$NON-NLS-1$
	private static final String TASK_MKDIR = "mkdir"; //$NON-NLS-1$
	private static final String TASK_DELETE = "delete"; //$NON-NLS-1$
	private static final String TASK_UNZIP = "unzip"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SRC = "src"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DEST = "dest"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DIR = "dir"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILE = "file"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VERBOSE = "verbose"; //$NON-NLS-1$
	private static final String ATTRIBUTE_IGNORE_ERRORS = "ignoreerrors"; //$NON-NLS-1$
	private static final String ATTRIBUTE_USE_TIMESTAMP = "usetimestamp"; //$NON-NLS-1$
	private static final String ATTRIBUTE_USERNAME = "username"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PASSWORD = "password"; //$NON-NLS-1$
	private static final String TAG_OPEN = "<"; //$NON-NLS-1$
	private static final String TAG_CLOSE = "/>"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#addTargets(org.eclipse.pde.build.IAntScript)
	 */
	public void addTargets(IAntScript script) {
		//
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveElementCall(java.util.Map, org.eclipse.core.runtime.IPath, org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {
		printGetTask(destination, script, entryInfos);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveFilesCall(java.util.Map, org.eclipse.core.runtime.IPath, java.lang.String[], org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveFilesCall(Map entryInfos, IPath destination, String[] files, IAntScript script) {
		//
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#parseMapFileEntry(java.lang.String, java.util.Properties, java.util.Map)
	 */
	public void parseMapFileEntry(String rawEntry, Properties overrideTags, Map entryInfos) throws CoreException {
		String url = rawEntry;
		if (rawEntry.indexOf(',') != -1) {
			StringTokenizer tokenizer = new StringTokenizer(rawEntry, SEPARATOR);
			if (tokenizer.hasMoreTokens())
				url = tokenizer.nextToken();
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				int index = token.indexOf('=');
				if (index == -1) {
					// invalid format...we require key=value...log and continue
					IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, NLS.bind(Messages.warning_problemsParsingMapFileEntry, rawEntry));
					BundleHelper.getDefault().getLog().log(status);
				} else {
					String key = token.substring(0, index).trim();
					String value = token.substring(index + 1).trim();
					entryInfos.put(key, value);
				}
			}
		}
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, NLS.bind(Messages.error_invalidURLInMapFileEntry, rawEntry)));
		}
		entryInfos.put(ATTRIBUTE_SRC, url);
	}

	/*
	 * Print out the Ant GET task to the Ant script.
	 */
	private void printGetTask(IPath destination, IAntScript script, Map entryInfos) {
		String src = (String) entryInfos.get(ATTRIBUTE_SRC);
		int index = src.lastIndexOf('/');
		String filename = index == -1 ? src : src.substring(index);

		String dest = (String) entryInfos.get(ATTRIBUTE_DEST);
		if (dest != null) {
			//if a dest was specified, make sure the parent directory exists
			script.printTabs();
			script.print(TAG_OPEN + TASK_MKDIR);
			script.printAttribute(ATTRIBUTE_DIR, new Path(dest).removeLastSegments(1).toOSString(), true);
			script.print(TAG_CLOSE);
			script.println();
		} else {
			// "dest" attribute is mandatory
			dest = destination.removeLastSegments(1).append(filename).toOSString();
		}

		// "src" attribute is mandatory
		script.printTabs();
		script.print(TAG_OPEN + TASK_GET);
		script.printAttribute(ATTRIBUTE_SRC, src, true);
		script.printAttribute(ATTRIBUTE_DEST, dest, true);

		// the rest of the attributes are optional so check if they exist before writing in the file
		String ignoreErrors = (String) entryInfos.get(ATTRIBUTE_IGNORE_ERRORS);
		if (ignoreErrors != null)
			script.printAttribute(ATTRIBUTE_IGNORE_ERRORS, ignoreErrors, false);

		String useTimestamp = (String) entryInfos.get(ATTRIBUTE_USE_TIMESTAMP);
		if (useTimestamp != null)
			script.printAttribute(ATTRIBUTE_USE_TIMESTAMP, useTimestamp, false);

		String verbose = (String) entryInfos.get(ATTRIBUTE_VERBOSE);
		if (verbose != null)
			script.printAttribute(ATTRIBUTE_VERBOSE, verbose, false);

		String username = (String) entryInfos.get(ATTRIBUTE_USERNAME);
		String password = (String) entryInfos.get(ATTRIBUTE_PASSWORD);
		if (username != null)
			script.printAttribute(ATTRIBUTE_USERNAME, username, password != null);
		if (password != null)
			script.printAttribute(ATTRIBUTE_PASSWORD, password, username != null);

		script.print(TAG_CLOSE);

		// if we have a feature or un-packed plug-in then we need to unzip it
		boolean unpack = Boolean.valueOf((String) entryInfos.get(UNPACK)).booleanValue();
		if (unpack || ELEMENT_TYPE_FEATURE.equals(entryInfos.get(KEY_ELEMENT_TYPE))) {
			Path destPath = new Path(dest);
			String unzipped = destPath.removeLastSegments(1).toOSString();
			if (destPath.getFileExtension().equalsIgnoreCase("jar")) { //$NON-NLS-1$
				unzipped = destPath.removeFileExtension().toOSString();
				script.printTabs();
				script.print(TAG_OPEN + TASK_MKDIR);
				script.printAttribute(ATTRIBUTE_DIR, unzipped, true);
				script.print(TAG_CLOSE);
			}
			script.printTabs();
			script.print(TAG_OPEN + TASK_UNZIP);
			script.printAttribute(ATTRIBUTE_SRC, dest, true);
			script.printAttribute(ATTRIBUTE_DEST, unzipped, true);
			script.print(TAG_CLOSE);

			script.printTabs();
			script.print(TAG_OPEN + TASK_DELETE);
			script.printAttribute(ATTRIBUTE_FILE, dest, true);
			script.print(TAG_CLOSE);
		}
		script.println();
	}

}
