/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeFeatureFactory;

/**
 * Used to create a build manifest file describing what plug-ins and versions
 * were included in a build. It has to be executed after a fetch task.
 */
public class BuildManifestTask extends Task implements IPDEBuildConstants, IXMLConstants {
	private String buildId;
	protected String buildName;
	private String buildQualifier;
	private String buildType;
	protected boolean children = true;
	protected String destination;
	protected Properties directory;
	protected String directoryLocation;
	protected String[] elements;
	protected String installLocation;

	/**
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		try {
			if (this.elements == null) {
				String message = TaskMessages.error_missingElement;
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, message, null));
			}
			readDirectory();
			try (PrintWriter output = new PrintWriter(new BufferedOutputStream(new FileOutputStream(destination)))) {
				List<String> entries = new ArrayList<>(20);
				for (String element : elements)
					collectEntries(entries, element);
				generatePrologue(output);
				generateEntries(output, entries);
			}
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	protected void generatePrologue(PrintWriter output) {
		output.print("# Build Manifest for "); //$NON-NLS-1$
		output.println(buildName);
		output.println();
		output.println("# The format of this file is:"); //$NON-NLS-1$
		output.println("# <type>@<element>=<CVS tag>"); //$NON-NLS-1$
		output.println();
		String id = getBuildId();
		if (id != null) {
			output.print(PROPERTY_BUILD_ID + "="); //$NON-NLS-1$
			output.println(id);
		}
		String type = getBuildType();
		if (type != null) {
			output.print(PROPERTY_BUILD_TYPE + "="); //$NON-NLS-1$
			output.println(type);
		}
		String qualifier = getBuildQualifier();
		if (qualifier != null) {
			output.print(PROPERTY_BUILD_QUALIFIER + "="); //$NON-NLS-1$
			output.println(qualifier);
		}
		output.println();
	}

	/**
	 * 
	 * @return String
	 */
	protected String getBuildId() {
		if (buildId == null)
			buildId = getProject().getProperty(PROPERTY_BUILD_ID);
		return buildId;
	}

	/**
	 * 
	 * @return String
	 */
	protected String getBuildQualifier() {
		if (buildQualifier == null)
			buildQualifier = getProject().getProperty(PROPERTY_BUILD_QUALIFIER);
		return buildQualifier;
	}

	/**
	 * 
	 * @return String
	 */
	protected String getBuildType() {
		if (buildType == null)
			buildType = getProject().getProperty(PROPERTY_BUILD_TYPE);
		return buildType;
	}

	protected void generateEntries(PrintWriter output, List<String> entries) {
		Collections.sort(entries);
		for (String entry : entries) {
			output.println(entry);
		}
	}

	/**
	 * Collects all the elements that are part of this build.
	 */
	protected void collectEntries(List<String> entries, String entry) throws CoreException {
		String cvsInfo = directory.getProperty(entry);
		if (cvsInfo == null) {
			String message = NLS.bind(TaskMessages.error_missingDirectoryEntry, entry);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}

		int index = entry.indexOf('@');
		String type = entry.substring(0, index);
		String element = entry.substring(index + 1);
		if (type.equals("plugin") || type.equals("fragment")) { //$NON-NLS-1$ //$NON-NLS-2$
			String[] cvsFields = Utils.getArrayFromString(cvsInfo);
			String tag = cvsFields[0];
			StringBuffer sb = new StringBuffer();
			sb.append(entry);
			sb.append("="); //$NON-NLS-1$
			sb.append(tag);
			entries.add(sb.toString());
		} else if (children && type.equals("feature")) { //$NON-NLS-1$
			BuildTimeFeature feature = readFeature(element);
			collectChildrenEntries(entries, feature);
		}
	}

	protected void collectChildrenEntries(List<String> entries, BuildTimeFeature feature) throws CoreException {
		FeatureEntry[] pluginEntries = feature.getPluginEntries();
		for (FeatureEntry pluginEntry : pluginEntries) {
			String elementId = pluginEntry.getId();
			if (pluginEntry.isFragment())
				collectEntries(entries, "fragment@" + elementId); //$NON-NLS-1$
			else
				collectEntries(entries, "plugin@" + elementId); //$NON-NLS-1$
		}
	}

	/**
	 * 
	 * @return Feature
	 */
	protected BuildTimeFeature readFeature(String element) throws CoreException {
		IPath root = IPath.fromOSString(installLocation);
		root = root.append(DEFAULT_FEATURE_LOCATION);
		root = root.append(element);
		try {
			BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
			return factory.createFeature(root.toPath(), null);
		} catch (Exception e) {
			String message = NLS.bind(TaskMessages.error_creatingFeature, element);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e));
		}
	}

	/**
	 * Sets the installLocation.
	 */
	public void setInstall(String installLocation) {
		this.installLocation = installLocation;
	}

	/**
	 * Reads directory file at the directoryLocation.
	 */
	protected void readDirectory() throws CoreException {
		try {
			directory = new Properties();
			File file = new File(directoryLocation);
			try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
				directory.load(is);
			}
		} catch (IOException e) {
			String message = NLS.bind(TaskMessages.error_readingDirectory, directoryLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, message, e));
		}
	}

	public void setDirectory(String directory) {
		directoryLocation = directory;
	}

	public void setElements(String value) {
		elements = Utils.getArrayFromString(value);
	}

	/**
	 * Sets the full location of the manifest file.
	 */
	public void setDestination(String value) {
		destination = value;
	}

	/**
	 * Whether children of this element should be taken into account.
	 */
	public void setChildren(boolean children) {
		this.children = children;
	}

	public void setBuildName(String value) {
		buildName = value;
	}

	/**
	 * Sets the buildId.
	 */
	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	/**
	 * Sets the buildQualifier.
	 */
	public void setBuildQualifier(String buildQualifier) {
		this.buildQualifier = buildQualifier;
	}

	/**
	 * Sets the buildType.
	 */
	public void setBuildType(String buildType) {
		this.buildType = buildType;
	}

}
