/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Karsten Thoms - Bug#535325
 *     Christoph Läubrich - Bug 568865 - [target] add advanced editing capabilities for custom target platforms
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

/**
 * A directory of bundles.
 *
 * @since 3.5
 */
public class DirectoryBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container
	 */
	public static final String TYPE = "Directory"; //$NON-NLS-1$

	/**
	 * Path to this container's directory in the local file system.
	 * The path may contain string substitution variables.
	 */
	private final String fPath;

	/**
	 * Constructs a directory bundle container at the given location.
	 *
	 * @param path directory location in the local file system, may contain string substitution variables
	 */
	public DirectoryBundleContainer(String path) {
		fPath = path;
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return getDirectory().toString();
		}
		return fPath;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		File dir = getDirectory();
		if (dir.isDirectory()) {
			File site = getSite(dir);
			File[] files = site.listFiles();
			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
			return Arrays.stream(files).parallel() //
					.map(file -> {
						localMonitor.split(1);
						try {
							return new TargetBundle(file);
						} catch (CoreException e) {
							// Ignore non-bundle files
							return null;
						}
					}).filter(Objects::nonNull) //
					.toArray(TargetBundle[]::new);
		}
		throw new CoreException(Status.error(NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		File dir = getDirectory();
		if (dir.isDirectory()) {
			File site = getFeatureSite(dir);
			File[] files = site.listFiles();
			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
			return Arrays.stream(files).parallel().map(file -> {
				localMonitor.split(1);
				try {
					return new TargetFeature(file);
				} catch (CoreException e) {
					// Ignore non-feature files
					return null;
				}
			}).filter(Objects::nonNull).toArray(TargetFeature[]::new);
		}
		throw new CoreException(Status.error(NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
	}

	/**
	 * Returns the directory to search for bundles in.
	 *
	 * @return directory if unable to resolve variables in the path
	 */
	protected File getDirectory() throws CoreException {
		String path = resolveVariables(fPath);
		return new File(path);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof DirectoryBundleContainer dbc) {
			return fPath.equals(dbc.fPath);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fPath.hashCode();
	}

	@Override
	public String toString() {
		return new StringBuilder("Directory ").append(fPath).toString(); //$NON-NLS-1$
	}

	/**
	 * Returns the directory to scan for bundles - a "plug-ins" sub directory if present.
	 *
	 * @param root the location the container specifies as a root directory
	 * @return the given directory or its plug-ins sub directory if present
	 */
	private File getSite(File root) {
		File file = new File(root, IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
		if (file.exists()) {
			return file;
		}
		return root;
	}

	/**
	 * Returns the directory to scan for features - a "features" sub directory
	 * if present.
	 *
	 * @param root
	 *            the location the container specifies as a root directory
	 * @return the given directory or its plug-ins sub directory if present
	 */
	private File getFeatureSite(File root) {
		File file = new File(root, IPDEBuildConstants.DEFAULT_FEATURE_LOCATION);
		if (file.exists()) {
			return file;
		}
		return root;
	}

	public void reload() {
		clearResolutionStatus();
	}

}
