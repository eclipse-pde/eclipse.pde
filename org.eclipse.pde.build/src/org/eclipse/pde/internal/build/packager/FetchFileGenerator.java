/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;

public class FetchFileGenerator extends AbstractScriptGenerator {
	private Config config;
	private String[] filters;
	private String mapLocation;
	private String collectedFiles;

	private Properties mapContent;
	private Properties selectedFiles;

	public void generate() throws CoreException {
		config = (Config) getConfigInfos().get(0);
		collectedFiles = ""; //$NON-NLS-1$

		openScript(workingDirectory, DEFAULT_FETCH_SCRIPT_FILENAME);
		generatePrologue();
		try {
			processMapFile();
			writeDirectory();
			generateEpilogue();
		} finally {
			closeScript();
		}
	}

	private void generatePrologue() {
		script.printProjectDeclaration("Packager' File fetcher", TARGET_MAIN, "."); //$NON-NLS-1$ //$NON-NLS-2$
		script.println();
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateEpilogue() {
		script.printTargetEnd();
		script.printProjectEnd();
		script.close();
	}

	public void generateFetchFileFor(String fileName, String baseurl, String loginInfo) {
		String login = null;
		String password = null;
		String[] login_password = Utils.getArrayFromString(loginInfo, ":"); //$NON-NLS-1$
		if (login_password.length == 2) {
			login = login_password[0];
			password = login_password[1];
		} else {
			//TODO Log an warning if the size is 1 
		}
		script.printGet(baseurl + fileName, getPropertyFormat(PROPERTY_DOWNLOAD_DIRECTORY) + "/" + fileName, login, password, true); //$NON-NLS-1$
	}

	public void setContentFilter(String filters) {
		this.filters = Utils.getArrayFromStringWithBlank(filters, "&"); //$NON-NLS-1$
	}

	public void setMapLocation(String mapLocation) {
		this.mapLocation = mapLocation;
	}

	private void writeDirectory() throws CoreException {
		selectedFiles = new Properties();
		selectedFiles.put(config.toString(","), collectedFiles); //$NON-NLS-1$

		try {
			OutputStream stream = new BufferedOutputStream(new FileOutputStream(workingDirectory + "/" + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR)); //$NON-NLS-1$
			try {
				selectedFiles.store(stream, null);
			} finally {
				stream.close();
			}
		} catch (FileNotFoundException e) {
			String message = Policy.bind("exception.writingFile", workingDirectory + "/" + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR); //$NON-NLS-1$ //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		} catch (IOException e) {
			String message = Policy.bind("exception.writingFile", workingDirectory + "/" + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR); //$NON-NLS-1$ //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	private void processMapFile() throws CoreException {
		final int URL = 0;
		final int CONFIGS = 1;
		final int DIRECTORY = 2;

		mapContent = readProperties(mapLocation, ""); //$NON-NLS-1$

		for (Iterator iter = mapContent.entrySet().iterator(); iter.hasNext();) {
			Map.Entry mapEntry = (Map.Entry) iter.next();
			String fileName = (String) mapEntry.getKey();
			String[] fileDescription = Utils.getArrayFromStringWithBlank((String) mapEntry.getValue(), "|"); //$NON-NLS-1$

			if (fileDescription.length < 4) {
				String message = Policy.bind("error.incorrectDirectoryEntry", (String) mapEntry.getKey() + "=" + (String) mapEntry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
			}

			// check if the entry can be used for the current config
			String[] entryConfigs = Utils.getArrayFromStringWithBlank(fileDescription[CONFIGS], "&"); //$NON-NLS-1$
			String userInfos = ""; //$NON-NLS-1$
			try {
				userInfos = new URL(fileDescription[URL]).getUserInfo();
			} catch (MalformedURLException e) {
				//TODO Should through an exception? and / or try to check the url with the file name concatenated?
			}
			if (entryConfigs.length == 0) {
				generateFetchFileFor(fileName, fileDescription[URL], userInfos);
				collectedFiles += fileName + ", " + (fileDescription[DIRECTORY].equals("") ? "." : fileDescription[DIRECTORY]) + " & "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}

			for (int i = 0; i < entryConfigs.length; i++) {
				Config aConfig = new Config(entryConfigs[i]);
				if (aConfig.equals(config) || aConfig.equals(Config.genericConfig())) { //$NON-NLS-1$
					generateFetchFileFor(fileName, fileDescription[URL], userInfos);
					collectedFiles += fileName + ", " + (fileDescription[DIRECTORY].equals("") ? "." : fileDescription[DIRECTORY]) + " & "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					break;
				}
			}

			//TODO Needs to add the filtering on the content 
		}
	}
}
