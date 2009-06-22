/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.build.*;

public class UnzipperGenerator extends AbstractScriptGenerator {
	private static final String DATA_SEPARATOR = "|"; //$NON-NLS-1$
	private static final String ENTRY_SEPARATOR = "%"; //$NON-NLS-1$
	private static final byte ARCHIVE_NAME = 0;
	private static final byte FOLDER = 1;
	private static final byte CONFIGS = 2;

	// The name the file containing the list of zips
	private String directoryLocation = DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR;
	// The list of zips. The key is the name of the zipfile, and the first property is the place to extract it
	private Properties zipsList;
	// The location of the packaging.properties file
	private String packagingPropertiesLocation;

	private String[] unzipOrder = new String[0];

	public void generate() throws CoreException {
		prepareGeneration();
		openScript(workingDirectory, DEFAULT_UNZIPPER_FILENAME_DESCRIPTOR);
		try {
			generatePrologue();
			generateUncompressionCommands();
			generateEpilogue();
		} finally {
			closeScript();
		}
	}

	/**
	 * 
	 */
	private void prepareGeneration() {
		if (packagingPropertiesLocation == null)
			return;

		Properties packagingProperties = new Properties();
		InputStream propertyStream = null;
		try {
			propertyStream = new BufferedInputStream(new FileInputStream(packagingPropertiesLocation));
			try {
				packagingProperties.load(propertyStream);
			} finally {
				propertyStream.close();
			}
		} catch (FileNotFoundException e) {
			//			String message = Policy.bind("exception.readingFile", packagingPropertiesLocation); //$NON-NLS-1$
			////			Log.throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		} catch (IOException e) {
			//			String message = Policy.bind("exception.readingFile", packagingPropertiesLocation); //$NON-NLS-1$
			//			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
		unzipOrder = Utils.getArrayFromStringWithBlank(packagingProperties.getProperty("unzipOrder", ""), ","); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	private void generateEpilogue() {
		script.printTargetEnd();
		script.println();
		script.printProjectEnd();
	}

	private void generatePrologue() {
		script.println();
		script.printComment("Unzip script"); //$NON-NLS-1$ 
		script.println();
		script.printProjectDeclaration("Unzipper", TARGET_MAIN, "."); //$NON-NLS-1$	//$NON-NLS-2$
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateUncompressionCommands() throws CoreException {
		zipsList = readProperties(workingDirectory, directoryLocation, IStatus.ERROR);

		String zipEntries = zipsList.getProperty("toUnzip", ""); //$NON-NLS-1$	//$NON-NLS-2$

		List toUnzipWithOrder = new ArrayList(unzipOrder.length);
		String[] allZipEntries = Utils.getArrayFromString(zipEntries, ENTRY_SEPARATOR);
		for (int i = 0; i < allZipEntries.length; i++) {
			String[] entryDetail = Utils.getArrayFromString(allZipEntries[i], DATA_SEPARATOR);
			script.printComment("Uncompress " + entryDetail[ARCHIVE_NAME]); //$NON-NLS-1$

			if (!entryDetail[FOLDER].equals(".")) //$NON-NLS-1$
				script.printMkdirTask("${tempDirectory}/" + entryDetail[FOLDER]); //$NON-NLS-1$

			if (delayed(entryDetail[ARCHIVE_NAME])) {
				toUnzipWithOrder.add(entryDetail);
				continue;
			}
			generateUncompress(entryDetail);
			script.println();
			script.println();
		}

		//Deal with the entries that have a specific order.
		for (int i = 0; i < unzipOrder.length; i++) {
			for (Iterator iter = toUnzipWithOrder.iterator(); iter.hasNext();) {
				String[] entry = (String[]) iter.next();
				if (entry[ARCHIVE_NAME].startsWith(unzipOrder[i])) {
					generateUncompress(entry);
					iter.remove();
				}
			}
		}
	}

	private void generateUncompress(String[] entryDetail) {
		if (entryDetail[ARCHIVE_NAME].endsWith(".zip")) { //$NON-NLS-1$
			generateUnzipArchive(entryDetail);
			generateUnzipRootFiles(entryDetail);
			return;
		}

		if (entryDetail[ARCHIVE_NAME].endsWith(".tar.gz") || entryDetail[ARCHIVE_NAME].endsWith(".tar")) { //$NON-NLS-1$ //$NON-NLS-2$
			generateUntarArchice(entryDetail);
			generateUntarRootFiles(entryDetail);
		}
	}

	private boolean delayed(String fileName) {
		for (int i = 0; i < unzipOrder.length; i++) {
			if (fileName.startsWith(unzipOrder[i]))
				return true;
		}
		return false;
	}

	private List getMatchingConfig(String[] entryDetail) {
		List applyingConfigs = null;
		if (entryDetail.length == 2) {
			applyingConfigs = getConfigInfos();
		} else {
			String[] configs = Utils.getArrayFromString(entryDetail[CONFIGS], "&"); //$NON-NLS-1$
			applyingConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				applyingConfigs.add(new Config(configs[i]));
			}
		}
		return applyingConfigs;
	}

	private void generateUnzipArchive(String[] entryDetail) {
		List parameters = new ArrayList(1);
		parameters.add("-o -X ${unzipArgs} "); //$NON-NLS-1$
		parameters.add(Utils.getPropertyFormat("downloadDirectory") + '/' + entryDetail[ARCHIVE_NAME]); //$NON-NLS-1$ 
		script.printExecTask("unzip", "${tempDirectory}/" + entryDetail[FOLDER], parameters, null, true); //$NON-NLS-1$//$NON-NLS-2$
	}

	//Uncompress the root files into a platform specific folder
	private void generateUnzipRootFiles(String[] entryDetail) {
		//Unzip the root files in a "config specific folder" for all the configurations that matched this entry
		for (Iterator iter = getMatchingConfig(entryDetail).iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			List parameters = new ArrayList(3);
			String rootFilesFolder = "${tempDirectory}/" + config.toString(".") + '/' + entryDetail[FOLDER]; //$NON-NLS-1$ //$NON-NLS-2$
			script.printMkdirTask(rootFilesFolder);
			parameters.add("-o -X ${unzipArgs} "); //$NON-NLS-1$
			parameters.add(Utils.getPropertyFormat("downloadDirectory") + '/' + entryDetail[ARCHIVE_NAME]); //$NON-NLS-1$ 
			parameters.add("-x " + (entryDetail[FOLDER].equals(".") ? "eclipse/" : "") + "features/*" + " " + (entryDetail[FOLDER].equals(".") ? "eclipse/" : "") + "plugins/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
			script.printExecTask("unzip", rootFilesFolder, parameters, null, true); //$NON-NLS-1$
		}
	}

	private void generateUntarArchice(String[] entryDetail) {
		List parameters = new ArrayList(2);
		parameters.add("-" + (entryDetail[ARCHIVE_NAME].endsWith(".gz") ? "z" : "") + "pxvf"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		parameters.add(Utils.getPropertyFormat("downloadDirectory") + '/' + entryDetail[ARCHIVE_NAME]); //$NON-NLS-1$ 
		script.printExecTask("tar", "${tempDirectory}/" + entryDetail[FOLDER], parameters, null, true); //$NON-NLS-1$//$NON-NLS-2$	
	}

	private void generateUntarRootFiles(String[] entryDetail) {
		//Unzip the root files in a "config specific folder" for all the configurations that matched this entry
		for (Iterator iter = getMatchingConfig(entryDetail).iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			List parameters = new ArrayList(4);
			String rootFilesFolder = "${tempDirectory}/" + config.toString(".") + '/' + entryDetail[FOLDER]; //$NON-NLS-1$ //$NON-NLS-2$
			script.printMkdirTask(rootFilesFolder);
			parameters.add("-" + (entryDetail[ARCHIVE_NAME].endsWith(".gz") ? "z" : "") + "pxvf"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			parameters.add(Utils.getPropertyFormat("downloadDirectory") + '/' + entryDetail[ARCHIVE_NAME]); //$NON-NLS-1$ 
			parameters.add("--exclude=" + (entryDetail[FOLDER].equals(".") ? "eclipse" : "") + "/features/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			parameters.add("--exclude=" + (entryDetail[FOLDER].equals(".") ? "eclipse" : "") + "/plugins/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			script.printExecTask("tar", rootFilesFolder, parameters, null, true); //$NON-NLS-1$
		}
	}

	public void setDirectoryLocation(String filename) {
		directoryLocation = filename;
	}

	/**
	 *  Set the property file containing information about packaging
	 * @param propertyFile the path to a property file
	 */
	public void setPropertyFile(String propertyFile) {
		packagingPropertiesLocation = propertyFile;
	}
}
