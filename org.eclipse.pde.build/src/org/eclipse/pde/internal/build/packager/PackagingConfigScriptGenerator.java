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
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;

public class PackagingConfigScriptGenerator extends AssembleConfigScriptGenerator {
	private Properties packagingProperties;

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
			script.printEchoTask("baseDir: " + getPropertyFormat(PROPERTY_BASEDIR));
			script.printEchoTask("tmpDir: " + getPropertyFormat("tempDirectory"));
			script.printEchoTask("collectingFolder: " + getPropertyFormat(PROPERTY_COLLECTING_FOLDER));
			script.printEchoTask("archivePrefix: " + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX));
			script.println("<echo message=\"eclipse.base: ${eclipse.base}\"/>");
			script.println("<echo message=\"tmp_dir: ${assemblyTempDir}\"/>");
			script.println("<echo message=\"destination.temp.folder ${destination.temp.folder}\"/>");
		}
		Map parameters = new HashMap(1);
		parameters.put("assembleScriptName", filename); //$NON-NLS-1$
		//TODO Improve the name handling
		script.printAntTask(getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, "assemble." + configInfo.toStringReplacingAny(".", ANY_STRING) + ".xml", null, null, parameters); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printTargetEnd();
	}

	private void generateAssembleTarget() throws CoreException {
		script.printTargetDeclaration("assemble", null, null, null, null); //$NON-NLS-1$
		generateZipRootFiles();
		generateZip();
		List args = new ArrayList(2);
		args.add("-R"); //$NON-NLS-1$
		args.add("700"); //$NON-NLS-1$
		args.add("."); //$NON-NLS-1$
		script.printExecTask("chmod", getPropertyFormat("tempDirectory") + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), args, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		script.printDeleteTask(getPropertyFormat("tempDirectory") + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), "**/*", null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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

	private void generateZip() throws CoreException {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + plugins[i].getUniqueId() + "_" + plugins[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			parameters.add(getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION + '/' + features[i].getVersionedIdentifier().toString()); //$NON-NLS-1$ //$NON-NLS-2$
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
			String message = Policy.bind("exception.readingFile", packagingPropertiesLocation); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		} catch (IOException e) {
			String message = Policy.bind("exception.readingFile", packagingPropertiesLocation); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
	}

}
