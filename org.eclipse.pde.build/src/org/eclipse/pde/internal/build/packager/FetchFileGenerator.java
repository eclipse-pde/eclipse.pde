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
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;

public class FetchFileGenerator extends AbstractScriptGenerator {
	private static final String ENTRY_SEPARATOR = "|"; //$NON-NLS-1$
	private static final String FILTER_SEPARATOR = "&"; //$NON-NLS-1$
	// Unknown component name
	private static final String UNKNOWN = "*"; //$NON-NLS-1$ 	

	private Config config;
	private String[] filters;
	private String mapLocation;
	private String collectedFiles;
	private String[] componentFilter;

	private Properties mapContent;
	private Properties selectedFiles;

	private void displayDebugInfo() {
		if (!BundleHelper.getDefault().isDebugging())
			return;

		System.out.println("Configuration: " + config.toString()); //$NON-NLS-1$
		System.out.println("Filters: " + (filters != null ? Utils.getStringFromArray(filters, ", ") : "NONE")); //$NON-NLS-1$ 	//$NON-NLS-2$ 	//$NON-NLS-3$
		System.out.println("Component filter: " + (componentFilter != null ? Utils.getStringFromArray(componentFilter, ", ") : "NONE")); //$NON-NLS-1$ 	//$NON-NLS-2$ 	//$NON-NLS-3$
		System.out.println("Map location: " + mapLocation); //$NON-NLS-1$
	}

	public void generate() throws CoreException {
		config = (Config) getConfigInfos().get(0);
		collectedFiles = ""; //$NON-NLS-1$
		displayDebugInfo();

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
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 1, Policy.bind("warning.missingPassword", fileName), null); //$NON-NLS-1$
			BundleHelper.getDefault().getLog().log(status);
		}
		script.printGet(baseurl + fileName, getPropertyFormat(PROPERTY_DOWNLOAD_DIRECTORY) + '/' + fileName, login, password, true);
	}

	public void setContentFilter(String filters) {
		this.filters = Utils.getArrayFromStringWithBlank(filters, FILTER_SEPARATOR); //$NON-NLS-1$
	}

	public void setMapLocation(String mapLocation) {
		this.mapLocation = mapLocation;
	}

	private void writeDirectory() throws CoreException {
		selectedFiles = new Properties();
		selectedFiles.put(config.toString(","), collectedFiles); //$NON-NLS-1$

		try {
			OutputStream stream = new BufferedOutputStream(new FileOutputStream(workingDirectory + '/' + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR)); //$NON-NLS-1$
			try {
				selectedFiles.store(stream, null);
			} finally {
				stream.close();
			}
		} catch (FileNotFoundException e) {
			String message = Policy.bind("exception.writingFile", workingDirectory + '/' + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		} catch (IOException e) {
			String message = Policy.bind("exception.writingFile", workingDirectory + '/' + DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	private void processMapFile() throws CoreException {
		final int URL = 0;
		final int CONFIGS = 1;
		final int DIRECTORY = 2;
		final int FILTERS = 3;
		final int COMPONENT = 4;

		mapContent = readProperties(mapLocation, "", IStatus.ERROR); //$NON-NLS-1$

		for (Iterator iter = mapContent.entrySet().iterator(); iter.hasNext();) {
			Map.Entry mapEntry = (Map.Entry) iter.next();
			String fileName = (String) mapEntry.getKey();
			String[] fileDescription = Utils.getArrayFromStringWithBlank((String) mapEntry.getValue(), ENTRY_SEPARATOR);

			if (fileDescription.length < 4) {
				String message = Policy.bind("error.incorrectDirectoryEntry", (String) mapEntry.getKey() + '=' + (String) mapEntry.getValue()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
			}

			// check if the entry can be used for the current config
			String userInfos = ""; //$NON-NLS-1$
			try {
				userInfos = new URL(fileDescription[URL]).getUserInfo();
			} catch (MalformedURLException e) {
				IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url", fileDescription[URL]), e); //$NON-NLS-1$
				throw new CoreException(status);
			}

			if (filterByConfig(fileDescription[CONFIGS]) && filterByFilter(fileDescription[FILTERS]) && filterByComponentName(fileDescription.length > 4 ? fileDescription[COMPONENT] : UNKNOWN)) {
				generateFetchFileFor(fileName, fileDescription[URL], userInfos);
				collectedFiles += fileName + ", " + (fileDescription[DIRECTORY].equals("") ? "." : fileDescription[DIRECTORY]) + " & "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$				
			} else {
				if (BundleHelper.getDefault().isDebugging()) {
					IStatus status = new Status(IStatus.INFO, PI_PDEBUILD, WARNING_ELEMENT_NOT_FETCHED, Policy.bind("error.fetchingFailed", fileDescription[DIRECTORY]), null); //$NON-NLS-1$
					BundleHelper.getDefault().getLog().log(status);
				}
			}
		}
	}

	//Return true if the filters specified to be packaged match the entry.
	//When no filter is specified on the entry or there is no filtering, then the file is fetched 
	private boolean filterByFilter(String filterString) {
		if (filters.length == 0)
			return true;

		String[] entryFilters = Utils.getArrayFromStringWithBlank(filterString, ","); //$NON-NLS-1$
		if (entryFilters.length == 0)
			return true;

		for (int i = 0; i < entryFilters.length; i++) {
			for (int j = 0; j < filters.length; j++) {
				if (filters[j].equals(entryFilters[i]))
					return true;
			}
		}
		return false;
	}

	//Return true, if the entryConfigs match the config we are packaging
	private boolean filterByConfig(String entryConfigString) {
		String[] entryConfigs = Utils.getArrayFromStringWithBlank(entryConfigString, FILTER_SEPARATOR);
		if (entryConfigs.length == 0 || config.equals(Config.genericConfig()))
			return true;

		for (int i = 0; i < entryConfigs.length; i++) {
			Config aConfig = new Config(entryConfigs[i]);
			if (aConfig.equals(config) || aConfig.equals(Config.genericConfig())) {
				return true;
			}
		}
		return false;
	}

	//Return true if the componentName is listed in the component filter, or if no filter is specified
	private boolean filterByComponentName(String componentName) {
		if (componentName.equals(UNKNOWN) || componentFilter == null)
			return true;

		for (int i = 0; i < componentFilter.length; i++) {
			if (componentFilter[i].equalsIgnoreCase(componentName) || componentFilter[i].equalsIgnoreCase(UNKNOWN))
				return true;
		}
		return false;
	}

	public void setComponentFilter(String componentFiler) {
		this.componentFilter = Utils.getArrayFromStringWithBlank(componentFiler, ",");
	}
}