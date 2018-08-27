/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;

/**
 * An API component for a system library.
 *
 * @since 1.0.0
 */
public class SystemLibraryApiComponent extends Component {

	/**
	 * Execution environment profile symbolic name.
	 */
	protected String[] fExecEnv;

	/**
	 * Associated library locations.
	 */
	protected LibraryLocation[] fLibraries;

	/**
	 * Home directory
	 */
	protected String fLocation;

	/**
	 * List of exported system packages
	 */
	protected String[] fSystemPackages;

	/**
	 * Language level - i.e. 1.4, 1.5, etc.
	 */
	protected String fVersion;

	/**
	 * Constructs a system library.
	 *
	 * @param baseline owning baseline
	 */
	protected SystemLibraryApiComponent(IApiBaseline baseline) {
		super(baseline);
	}

	/**
	 * Constructs a system library from the given execution environment
	 * description file.
	 *
	 * @param baseline owning baseline
	 * @param description EE file
	 * @param systemPackages exported system packages
	 * @exception CoreException if unable to read the execution environment
	 *                description file
	 */
	public SystemLibraryApiComponent(IApiBaseline baseline, ExecutionEnvironmentDescription description, String[] systemPackages) throws CoreException {
		super(baseline);
		init(description);
		fSystemPackages = systemPackages;
	}

	@Override
	protected IApiDescription createApiDescription() throws CoreException {
		IApiDescription api = new ApiDescription(getSymbolicName());
		for (String fSystemPackage : fSystemPackages) {
			IPackageDescriptor pkg = Factory.packageDescriptor(fSystemPackage);
			api.setVisibility(pkg, VisibilityModifiers.API);
		}
		// have to fill in java.* as well
		String[] packageNames = getPackageNames();
		for (String packageName : packageNames) {
			// for java 9
			if (packageName.startsWith("classes.java.")) { //$NON-NLS-1$
				packageName = packageName.substring(8);
			}
			if (packageName.startsWith("java.")) { //$NON-NLS-1$
				IPackageDescriptor pkg = Factory.packageDescriptor(packageName);
				api.setVisibility(pkg, VisibilityModifiers.API);
			}
		}
		return api;
	}

	@Override
	protected IApiFilterStore createApiFilterStore() {
		// TODO
		return null;
	}

	@Override
	protected List<IApiTypeContainer> createApiTypeContainers() throws CoreException {
		List<IApiTypeContainer> libs = new ArrayList<>(fLibraries.length);
		for (LibraryLocation lib : fLibraries) {
			libs.add(new ArchiveApiTypeContainer(this, lib.getSystemLibraryPath().toOSString()));
		}
		if (fLibraries.length == 0) {
			if (fLocation != null) {
				IPath newPath = new Path(fLocation);
				newPath = newPath.append("jmods").append("java.base.jmod"); //$NON-NLS-1$ //$NON-NLS-2$
				if (newPath.toFile().exists()) {
					libs.add(new ArchiveApiTypeContainer(this, newPath.toOSString()));
				}
			}
		}
		return libs;
	}

	@Override
	public String[] getExecutionEnvironments() {
		return fExecEnv;
	}

	@Override
	public String getSymbolicName() {
		return fExecEnv[0];
	}

	@Override
	public String getLocation() {
		return fLocation;
	}

	@Override
	public IRequiredComponentDescription[] getRequiredComponents() {
		return new IRequiredComponentDescription[0];
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	/**
	 * Initializes properties from the EE file.
	 *
	 * @param description EE file
	 */
	private void init(ExecutionEnvironmentDescription description) {
		fLibraries = description.getLibraryLocations();
		fExecEnv = new String[] { description.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL) };
		fVersion = fExecEnv[0];
		setName(fExecEnv[0]);
		fLocation = description.getProperty(ExecutionEnvironmentDescription.JAVA_HOME);
	}

	@Override
	public boolean isSourceComponent() {
		return false;
	}

	@Override
	public boolean isSystemComponent() {
		return true;
	}

	@Override
	public boolean isFragment() {
		return false;
	}

	@Override
	public boolean hasFragments() {
		return false;
	}

	public String getOrigin() {
		return null;
	}

	@Override
	public boolean hasApiDescription() {
		return false;
	}

	@Override
	public String[] getLowestEEs() {
		return null;
	}

	@Override
	public ResolverError[] getErrors() throws CoreException {
		return null;
	}
}
