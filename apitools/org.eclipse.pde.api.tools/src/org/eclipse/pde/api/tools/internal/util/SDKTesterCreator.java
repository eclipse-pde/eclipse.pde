/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;


public class SDKTesterCreator {
	private static final String DEBUG_PROPERTY = "DEBUG"; //$NON-NLS-1$

	private static final String OVERRIDE = "-o"; //$NON-NLS-1$
	private static boolean DEBUG = false;

	static final CRC32 CRC32 = new CRC32();

	static boolean override = false;

	public static void main(String[] args) {
		DEBUG = System.getProperty(DEBUG_PROPERTY) != null;
		// first argument is the component.xml root directory name
		if (args.length < 2) {
			return;
		}
		if (args.length == 3) {
			if (args[2].equals(OVERRIDE)) {
				override = true;
			}
		}
		File componentXmlRoot = new File(args[0]);
		if (!componentXmlRoot.exists() || !componentXmlRoot.isDirectory()) {
			System.err.println("Invalid component.xml root : " + args[0]); //$NON-NLS-1$
			return;
		}
		File[] allComponentXmlFiles = Util.getAllFiles(componentXmlRoot, new FileFilter() {
			public boolean accept(File path) {
				return (path.isFile() && path.getName().equals(IApiCoreConstants.COMPONENT_XML_NAME)) || path.isDirectory();
			}
		});
		if (allComponentXmlFiles == null) {
			System.err.println("No component.xml to process"); //$NON-NLS-1$
			return;
		}
		String[] allComponentXmlFileNames = new String[allComponentXmlFiles.length];
		int beginIndex = componentXmlRoot.getAbsolutePath().length() + 1;
		for (int i = 0, max = allComponentXmlFiles.length; i < max; i++) {
			File file = allComponentXmlFiles[i];
			allComponentXmlFileNames[i] = file.getAbsolutePath().substring(beginIndex);
		}
		Arrays.sort(allComponentXmlFileNames, new Comparator() {
			public int compare(Object o1, Object o2) {
				String fileName1 = (String) o1;
				String fileName2 = (String) o2;
				return fileName1.compareTo(fileName2);
			}
		});
		if (DEBUG) {
			int max = allComponentXmlFileNames.length;
			System.out.println("" + max + " files found"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < max; i++) {
				System.out.println("component.xml [" + i + "] = " + allComponentXmlFileNames[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// second argument is the root of a plugins folder
		File pluginsRoot = new File(args[1]);
		if (!pluginsRoot.exists() || !pluginsRoot.isDirectory()) {
			System.err.println("Invalid plugins root : " + args[1]); //$NON-NLS-1$
			return;
		}
		File[] allPluginsFiles = pluginsRoot.listFiles();
		if (DEBUG) {
			if (allPluginsFiles != null) {
				int max = allPluginsFiles.length;
				System.out.println("" + max + " plugins found"); //$NON-NLS-1$ //$NON-NLS-2$
				for (int i = 0, max2 = allPluginsFiles.length; i < max2; i++) {
					File file = allPluginsFiles[i];
					System.out.println(file.getAbsolutePath());
				}
			}
		}
		if (allPluginsFiles == null) {
			System.err.println("No plugins to process"); //$NON-NLS-1$
			return;
		}
		
		List list = new ArrayList();
		for (int i = 0, max = allPluginsFiles.length; i < max; i++) {
			File file = allPluginsFiles[i];
			int result = getComponentXmlIndex(allComponentXmlFileNames, file);
			if (result >= 0) {
				list.add(new Integer(result));
				// there is a component.xml for the current plugin, add it
				if (file.isDirectory()) {
					// the plugin is a directory
					processDirectoryEntry(file, new File(componentXmlRoot, allComponentXmlFileNames[result]));
				} else {
					// the plugin is a jar'd plugin
					File outputFile = new File(file.getParent(), file.getName() + "_0");//$NON-NLS-1$
					if (processJarFile(file, outputFile, new File(componentXmlRoot, allComponentXmlFileNames[result]))) {
						// component.xml was added successfully into the plugin jar
						File oldFile = new File(file.getParent(), file.getName() + "_old"); //$NON-NLS-1$
						if (Util.copy(file, oldFile)) {
							// backup copy is created in oldFile
							if(file.delete()) {
								if (outputFile.renameTo(file)) {
									// get rid of the old copy
									oldFile.delete();
								} else {
									System.err.println("Rename " + outputFile + " to " + file + " failed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									outputFile.delete();
									// restore the backup file
									oldFile.renameTo(file);
								}
							} else {
								System.err.println("Could not delete " + file); //$NON-NLS-1$
								// delete new file and backup
								outputFile.delete();
								oldFile.delete();
							}
						} else {
							System.err.println("Could not create a backup for " + file); //$NON-NLS-1$
						}
					} else {
						// delete the local file since nothing has been done for it
						outputFile.delete();
					}
				}
			}
		}
		// find all component.xml that were not matched
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Integer index = (Integer) iterator.next();
			allComponentXmlFileNames[index.intValue()] = null;
		}
		for (int i = 0, max = allComponentXmlFileNames.length; i < max; i++) {
			String componentXmlFileName = allComponentXmlFileNames[i];
			if (componentXmlFileName != null) {
				System.err.println("This component.xml file " + componentXmlFileName + " was not matched"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private static void processDirectoryEntry(File file, File componentXmlFile) {
		File currentComponentXmlFile = new File(file, IApiCoreConstants.COMPONENT_XML_NAME);
		if (currentComponentXmlFile.exists()) {
			// there is a component.xml file in this plugin
			System.out.println("component.xml already exists"); //$NON-NLS-1$
			if (override) {
				System.out.println("overriding component.xml for " + file); //$NON-NLS-1$
				if (!currentComponentXmlFile.delete()) {
					System.err.println("Could not delete existing component.xml file (" + currentComponentXmlFile + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			} else {
				return;
			}
		}
		if(DEBUG) {
			System.out.println("Add component.xml in " + file); //$NON-NLS-1$
		}
		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(componentXmlFile));
			byte[] bytes = Util.getInputStreamAsByteArray(inputStream, -1);
			outputStream = new BufferedOutputStream(new FileOutputStream(new File(file, IApiCoreConstants.COMPONENT_XML_NAME)));
			outputStream.write(bytes);
			outputStream.flush();
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch(IOException e) {
				// ignore
			}
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch(IOException e) {
				// ignore
			}
		}
	}

	private static boolean processJarFile(File inputFile, File outputFile, File componentXMLFile) {
		ZipInputStream inputStream = null;
		ZipOutputStream zipOutputStream = null;
		try {
			zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		}
		if (zipOutputStream == null) {
			System.err.println("Could not create the output file : " + outputFile); //$NON-NLS-1$
			return false;
		}
		try {
			inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
			if (processArchiveEntry(inputStream, zipOutputStream, componentXMLFile) && DEBUG) {
				System.out.println("Add component.xml in " + inputFile); //$NON-NLS-1$
				return true;
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			try {
				zipOutputStream.flush();
				zipOutputStream.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return false;
	}

	private static boolean processArchiveEntry(ZipInputStream inputStream, ZipOutputStream zipOutputStream, File componentXmlFile)
			throws IOException {
		byte[] bytes = null;
		ZipEntry zipEntry = inputStream.getNextEntry();
		boolean containsComponentXML = false;
		while (zipEntry != null) {
			String name = zipEntry.getName();
			bytes = Util.getInputStreamAsByteArray(inputStream, (int) zipEntry.getSize());
			if (name.equals(IApiCoreConstants.COMPONENT_XML_NAME)) {
				containsComponentXML = true;
				if (override) {
					bytes = null;
					containsComponentXML = false;
				}
			}
			if (bytes != null) {
				writeZipFileEntry(zipOutputStream, name, bytes, CRC32);
			}
			inputStream.closeEntry();
			zipEntry = inputStream.getNextEntry();
		}
		if (!containsComponentXML && componentXmlFile != null) {
			// add component.xml entry
			BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(componentXmlFile));
			bytes = Util.getInputStreamAsByteArray(bufferedInputStream, -1);
			bufferedInputStream.close();
			if (bytes != null) {
				writeZipFileEntry(zipOutputStream, IApiCoreConstants.COMPONENT_XML_NAME, bytes, CRC32);
				return true;
			} else {
				System.err.println("Could not retrieve the contents of " + componentXmlFile); //$NON-NLS-1$
			}
		}
		return false;
	}

	private static void writeZipFileEntry(ZipOutputStream outputStream,
			String entryName,
			byte[] bytes,
			CRC32 crc32) throws IOException {
		crc32.reset();
		int byteArraySize = bytes.length;
		crc32.update(bytes, 0, byteArraySize);
		ZipEntry entry = new ZipEntry(entryName);
		entry.setMethod(ZipEntry.DEFLATED);
		entry.setSize(byteArraySize);
		entry.setCrc(crc32.getValue());
		outputStream.putNextEntry(entry);
		outputStream.write(bytes, 0, byteArraySize);
		outputStream.closeEntry();
	}

	private static int getComponentXmlIndex(String[] allComponentXmlFileNames, File file) {
		String fileName = file.getName();
		int index = fileName.indexOf('_');
		String key = (index == -1 ? fileName : fileName.substring(0, index)) + File.separator + IApiCoreConstants.COMPONENT_XML_NAME;
		int result = Arrays.binarySearch(allComponentXmlFileNames, key);
		return result;
	}
}
