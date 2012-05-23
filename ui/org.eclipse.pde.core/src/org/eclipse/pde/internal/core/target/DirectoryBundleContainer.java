/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.PDECore;

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
	private String fPath;

	/**
	 * Constructs a directory bundle container at the given location.
	 * 
	 * @param path directory location in the local file system, may contain string substitution variables
	 */
	public DirectoryBundleContainer(String path) {
		fPath = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return getDirectory().toString();
		}
		return fPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		File dir = getDirectory();
		if (dir.isDirectory()) {
			File site = getSite(dir);
			File[] files = site.listFiles();
			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
			List bundles = new ArrayList(files.length);
			for (int i = 0; i < files.length; i++) {
				if (localMonitor.isCanceled()) {
					return new TargetBundle[0];
				}
				try {
					TargetBundle rb = new TargetBundle(files[i]);
					if (rb != null) {
						bundles.add(rb);
					}
				} catch (CoreException e) {
					// ignore files that are not valid bundles (users may have non-bundle files in their target directory)
				}
				localMonitor.worked(1);
			}
			localMonitor.done();
			return (TargetBundle[]) bundles.toArray(new TargetBundle[bundles.size()]);
		}
		throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#resolveFeatures(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (definition instanceof TargetDefinition) {
			return ((TargetDefinition) definition).resolveFeatures(getLocation(false), monitor);
		}
		return new TargetFeature[0];
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof DirectoryBundleContainer) {
			DirectoryBundleContainer dbc = (DirectoryBundleContainer) container;
			return fPath.equals(dbc.fPath);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer().append("Directory ").append(fPath).toString(); //$NON-NLS-1$
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

}
