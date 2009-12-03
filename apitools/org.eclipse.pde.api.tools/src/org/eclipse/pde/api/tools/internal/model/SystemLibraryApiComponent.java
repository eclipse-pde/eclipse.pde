/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
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
	protected SystemLibraryApiComponent(IApiBaseline baseline){
		super(baseline);
	}
	/**
	 * Constructs a system library from the given execution environment description file.
	 * 
	 * @param baseline owning baseline
	 * @param description EE file
	 * @param systemPackages exported system packages
	 * @exception CoreException if unable to read the execution environment description file
	 */
	public SystemLibraryApiComponent(IApiBaseline baseline, ExecutionEnvironmentDescription description, String[] systemPackages) throws CoreException {
		super(baseline);
		init(description);
		fSystemPackages = systemPackages;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		IApiDescription api = new ApiDescription(getSymbolicName());
		for (int i = 0; i < fSystemPackages.length; i++) {
			IPackageDescriptor pkg  = Factory.packageDescriptor(fSystemPackages[i]);
			api.setVisibility(pkg, VisibilityModifiers.API);
		}
		// have to fill in java.* as well
		String[] packageNames = getPackageNames();
		for (int i = 0; i < packageNames.length; i++) {
			if (packageNames[i].startsWith("java.")) { //$NON-NLS-1$
				IPackageDescriptor pkg  = Factory.packageDescriptor(packageNames[i]);
				api.setVisibility(pkg, VisibilityModifiers.API);
			}
		}
		return api;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiComponent#createApiFilterStore()
	 */
	protected IApiFilterStore createApiFilterStore() {
		//TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createClassFileContainers()
	 */
	protected List createApiTypeContainers() throws CoreException {
		List libs = new ArrayList(fLibraries.length);
		for (int i = 0; i < fLibraries.length; i++) {
			LibraryLocation lib = fLibraries[i];
			libs.add(new ArchiveApiTypeContainer(this, lib.getSystemLibraryPath().toOSString()));
		}
		return libs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getExecutionEnvironments()
	 */
	public String[] getExecutionEnvironments() {
		return fExecEnv;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getId()
	 */
	public String getSymbolicName() {
		return fExecEnv[0];
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getLocation()
	 */
	public String getLocation() {
		return fLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getRequiredComponents()
	 */
	public IRequiredComponentDescription[] getRequiredComponents() {
		return new IRequiredComponentDescription[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}
	
	/**
	 * Initializes properties from the EE file.
	 * 
	 * @param description EE file
	 */
	private void init(ExecutionEnvironmentDescription description) throws CoreException {
		fLibraries = description.getLibraryLocations();
		fExecEnv = new String[]{description.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL)};
		fVersion = fExecEnv[0];
		setName(fExecEnv[0]);
		fLocation = description.getProperty(ExecutionEnvironmentDescription.JAVA_HOME);
	}

	/* (non-Javadoc)
	 * @see IApiComponent#isSourceComponent()
	 */
	public boolean isSourceComponent() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#isSystemComponent()
	 */
	public boolean isSystemComponent() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#isFragment()
	 */
	public boolean isFragment() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#hasFragments()
	 */
	public boolean hasFragments() {
		return false;
	}

	public String getOrigin() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#hasApiDescription()
	 */
	public boolean hasApiDescription() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#getSystemApiDescription(int)
	 */
	public IApiDescription getSystemApiDescription(int eeValue) throws CoreException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#getLowestEEs()
	 */
	public String[] getLowestEEs() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#getErrors()
	 */
	public ResolverError[] getErrors() throws CoreException {
		return null;
	}
}
