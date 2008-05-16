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
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.launching.EEVMType;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;

/**
 * An API component for a system library.
 * 
 * @since 1.0.0
 */
public class SystemLibraryApiComponent extends AbstractApiComponent {
	
	/**
	 * Execution environment profile symbolic name.
	 */
	private String[] fExecEnv;
	
	/**
	 * Associated library locations.
	 */
	private LibraryLocation[] fLibraries;
	
	/**
	 * Home directory
	 */
	private String fLocation;
	
	/**
	 * List of exported system packages
	 */
	private String[] fSystemPackages;
	
	/**
	 * Language level - i.e. 1.4, 1.5, etc.
	 */
	private String fVersion;
	
	/**
	 * Constructs a system library from the given execution environment description file.
	 * 
	 * @param profile owning profile
	 * @param description EE file
	 * @param systemPackages exported system packages
	 */
	public SystemLibraryApiComponent(IApiProfile profile, File description, String[] systemPackages) {
		super(profile);
		init(description);
		fSystemPackages = systemPackages;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		IApiDescription api = new ApiDescription(getId());
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
	protected List createClassFileContainers() throws CoreException {
		List libs = new ArrayList(fLibraries.length);
		for (int i = 0; i < fLibraries.length; i++) {
			LibraryLocation lib = fLibraries[i];
			libs.add(new ArchiveClassFileContainer(lib.getSystemLibraryPath().toOSString(), null));
		}
		return libs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getExecutionEnvironments()
	 */
	public String[] getExecutionEnvironments() {
		return fExecEnv;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getId()
	 */
	public String getId() {
		return fExecEnv[0];
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getLocation()
	 */
	public String getLocation() {
		return fLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getName()
	 */
	public String getName() {
		return fExecEnv[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getRequiredComponents()
	 */
	public IRequiredComponentDescription[] getRequiredComponents() {
		return new IRequiredComponentDescription[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}
	
	/**
	 * Initializes properties from the EE file.
	 * 
	 * @param description EE file
	 */
	private void init(File description) {
		EEVMType.clearProperties(description);
		fLibraries = EEVMType.getLibraryLocations(description);
		fExecEnv = new String[]{EEVMType.getProperty(EEVMType.PROP_CLASS_LIB_LEVEL, description)};
		fVersion = fExecEnv[0];
		fLocation = EEVMType.getProperty(EEVMType.PROP_JAVA_HOME, description);
	}

	/* (non-Javadoc)
	 * @see IApiComponent#isSourceComponent()
	 */
	public boolean isSourceComponent() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#isSystemComponent()
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
}
