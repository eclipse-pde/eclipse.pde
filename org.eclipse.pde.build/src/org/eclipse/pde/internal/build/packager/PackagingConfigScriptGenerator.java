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
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.*;

public class PackagingConfigScriptGenerator extends AssembleConfigScriptGenerator {
	private Properties packagingProperties;
	private String[] rootFiles;
	private String[] rootDirs;
	private String output;

	public void generate() throws CoreException {
		generatePrologue();
		generateMainTarget();
		generateAssembleTarget();
		generateEpilogue();
	}

	private void generatePrologue() {
		script.printProjectDeclaration("Package " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
		script.printProperty(PROPERTY_ARCHIVE_NAME, computeArchiveName());
	}

	private void generateMainTarget() {
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);

		if (BundleHelper.getDefault().isDebugging()) {
			script.printEchoTask(PROPERTY_BASEDIR + ": " + getPropertyFormat(PROPERTY_BASEDIR)); //$NON-NLS-1$
			script.printEchoTask("tmpDir: " + getPropertyFormat("tempDirectory")); //$NON-NLS-1$//$NON-NLS-2$
			script.printEchoTask(PROPERTY_COLLECTING_FOLDER + ": " + getPropertyFormat(PROPERTY_COLLECTING_FOLDER)); //$NON-NLS-1$
			script.printEchoTask(PROPERTY_ARCHIVE_PREFIX + ": " + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX)); //$NON-NLS-1$
			script.printEchoTask(PROPERTY_ECLIPSE_BASE + ": " + getPropertyFormat(PROPERTY_ECLIPSE_BASE)); //$NON-NLS-1$
			script.printEchoTask(PROPERTY_ASSEMBLY_TMP + ": " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP)); //$NON-NLS-1$
			script.printEchoTask(PROPERTY_DESTINATION_TEMP_FOLDER + ": " + getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER)); //$NON-NLS-1$
		}
		script.println("<condition property=\"" + PROPERTY_PLUGIN_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_PLUGIN_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_PLUGIN_ARCHIVE_PREFIX, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION);

		script.println();
		script.println("<condition property=\"" + PROPERTY_FEATURE_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_FEATURE_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_FEATURE_ARCHIVE_PREFIX, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION);

		Map parameters = new HashMap(1);
		parameters.put("assembleScriptName", filename); //$NON-NLS-1$
		//TODO Improve the name handling
		script.printAntTask(getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, "assemble." + configInfo.toStringReplacingAny(".", ANY_STRING) + ".xml", null, null, parameters); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printTargetEnd();
	}

	private void generateAssembleTarget() {
		script.printTargetDeclaration("assemble", null, null, null, null); //$NON-NLS-1$
		if (output.equalsIgnoreCase("tarGz")) { //$NON-NLS-1$
			generateAntTarTarget();
		} else if (output.equalsIgnoreCase("antZip")) { //$NON-NLS-1$
			generateAntZipTarget();
		} else if (output.equalsIgnoreCase("folder")) { //$NON-NLS-1$
			generateFolderTarget();
		} else { //By default use zip.exe
			generateZipRootFiles();
			generateZip();
			List args = new ArrayList(2);
			args.add("-R"); //$NON-NLS-1$
			args.add("700"); //$NON-NLS-1$
			args.add("."); //$NON-NLS-1$
			script.printExecTask("chmod", getPropertyFormat("tempDirectory") + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), args, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			script.printDeleteTask(getPropertyFormat("tempDirectory") + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), "**/*", null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		script.printTargetEnd();
	}

	private void generateZipRootFiles() {
		String fileList = packagingProperties.getProperty("root", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (!configInfo.equals(Config.genericConfig()))
			fileList += (fileList.length() == 0 ? "" : ",") + packagingProperties.getProperty("root." + configInfo.toString("."), ""); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

		String[] files = Utils.getArrayFromString(fileList, ","); //$NON-NLS-1$
		List parameters = new ArrayList(1);
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			if (file.startsWith("file:")) { //$NON-NLS-1$
				IPath target = new Path(file.substring(5));
				file = target.removeLastSegments(1).toOSString();
			}
			parameters.add(getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + file); //$NON-NLS-1$
			createZipExecCommand(parameters);
			parameters.clear();
		}
	}

	private void generateZip() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + plugins[i].getSymbolicName() + "_" + plugins[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < features.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + features[i].getVersionedIdentifier().toString()); //$NON-NLS-1$ //$NON-NLS-2$
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}
	}

	private void createZipExecCommand(List parameters) {
		parameters.add(0, "-r -q " + getPropertyFormat(PROPERTY_ZIP_ARGS) + " " + getPropertyFormat(PROPERTY_ARCHIVE_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("zip", getPropertyFormat("tempDirectory"), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void generateEpilogue() {
		script.printProjectEnd();
		script.close();
	}

	public void setPackagingPropertiesLocation(String packagingPropertiesLocation) throws CoreException {
		packagingProperties = new Properties();
		if (packagingPropertiesLocation == null || packagingPropertiesLocation.equals("")) //$NON-NLS-1$
			return;

		InputStream propertyStream = null;
		try {
			propertyStream = new BufferedInputStream(new FileInputStream(packagingPropertiesLocation));
			try {
				packagingProperties.load(new BufferedInputStream(propertyStream));
			} finally {
				propertyStream.close();
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
	}

	private boolean isFolder(Path pluginLocation) {
		return pluginLocation.toFile().isDirectory();
	}

	private void generateAntTarTarget() {
		int index = 0;
		FileSet[] files = new FileSet[plugins.length + features.length + rootFiles.length + rootDirs.length];
		if (files.length == 0)
			return;

		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			boolean isFolder = isFolder(pluginLocation);
			files[index++] = new TarFileSet(pluginLocation.toOSString(), !isFolder, null, null, null, null, null, getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + pluginLocation.lastSegment(), null);
		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getURL().getPath()); // Here we assume that all the features are local
			featureLocation = featureLocation.removeLastSegments(1);
			files[index++] = new TarFileSet(featureLocation.toOSString(), false, null, null, null, null, null, getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + featureLocation.lastSegment(), null);
		}

		if (rootFileProviders.size() == 0) {
			FileSet[] filesCorrectSize = new FileSet[plugins.length + features.length];
			System.arraycopy(files, 0, filesCorrectSize, 0, plugins.length + features.length);
			script.printTarTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, files);
			return;
		}

		for (int i = 0; i < rootFiles.length; i++) {
			IPath filePath = new Path(rootFiles[i]);
			files[index++] = new TarFileSet(filePath.toOSString(), true, null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + filePath.lastSegment(), null);
		}
		for (int i = 0; i < rootDirs.length; i++) {
			IPath dirPath = new Path(rootDirs[i]);
			files[index++] = new TarFileSet(dirPath.toOSString(), false, null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + dirPath.lastSegment(), null);
		}
		script.printTarTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, files);
	}

	private void generateAntZipTarget() {
		int index = 0;
		FileSet[] files = new FileSet[plugins.length + features.length + rootFiles.length + rootDirs.length];
		if (files.length == 0)
			return;

		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			boolean isFolder = isFolder(pluginLocation);
			files[index++] = new ZipFileSet(pluginLocation.toOSString(), !isFolder, null, null, null, null, null, getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + pluginLocation.lastSegment(), null);
		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getURL().getPath()); // Here we assume that all the features are local
			featureLocation = featureLocation.removeLastSegments(1);
			files[index++] = new ZipFileSet(featureLocation.toOSString(), false, null, null, null, null, null, getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + featureLocation.lastSegment(), null);
		}

		if (rootFileProviders.size() == 0) {
			FileSet[] filesCorrectSize = new FileSet[plugins.length + features.length];
			System.arraycopy(files, 0, filesCorrectSize, 0, plugins.length + features.length);
			script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, files);
			return;
		}

		for (int i = 0; i < rootFiles.length; i++) {
			IPath filePath = new Path(rootFiles[i]);
			files[index++] = new ZipFileSet(filePath.toOSString(), true, null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + filePath.lastSegment(), null);
		}
		for (int i = 0; i < rootDirs.length; i++) {
			IPath dirPath = new Path(rootDirs[i]);
			files[index++] = new ZipFileSet(dirPath.toOSString(), false, null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + dirPath.lastSegment(), null);
		}
		script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, files);
	}

	private void generateFolderTarget() {
		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			boolean isFolder = isFolder(pluginLocation);
			if (isFolder) {
				script.printCopyTask(null, getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + pluginLocation.lastSegment(), new FileSet[] {new FileSet(pluginLocation.toOSString(), null, null, null, null, null, null)}, false);
			} else {
				script.printCopyTask(pluginLocation.toOSString(), getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + pluginLocation.lastSegment(), null, false);
			}
		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getURL().getPath()); // Here we assume that all the features are local
			featureLocation = featureLocation.removeLastSegments(1);
			script.printCopyTask(null, getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + featureLocation.lastSegment(), new FileSet[] {new FileSet(featureLocation.toOSString(), null, null, null, null, null, null)}, false);
		}

		for (int i = 0; i < rootFiles.length; i++) {
			IPath filePath = new Path(rootFiles[i]);
			script.printCopyTask(filePath.toOSString(), getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, false);
		}

		for (int i = 0; i < rootDirs.length; i++) {
			IPath dirPath = new Path(rootDirs[i]);
			script.printCopyTask(null, getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + dirPath.lastSegment(), new FileSet[] {new FileSet(dirPath.toOSString(), null, null, null, null, null, null)}, false);
		}
	}

	public void rootFiles(String[] root) {
		this.rootFiles = root;
	}

	public void rootDirs(String[] root) {
		this.rootDirs = root;
	}

	public void setOutput(String outputFormat) {
		this.output = outputFormat;
	}
}