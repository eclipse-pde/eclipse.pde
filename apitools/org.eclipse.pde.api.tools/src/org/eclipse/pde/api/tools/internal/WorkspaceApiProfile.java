/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Handle to underlying workspace profile.
 * 
 * @since 1.0
 */
public class WorkspaceApiProfile implements IApiProfile {
	
	IApiProfile getUnderlyingProfile() {
		return ((ApiProfileManager)ApiPlugin.getDefault().getApiProfileManager()).getBaseWorkspaceProfile();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#addApiComponents(org.eclipse.pde.api.tools.internal.provisional.IApiComponent[], boolean)
	 */
	public void addApiComponents(IApiComponent[] components, boolean enabled) {
		getUnderlyingProfile().addApiComponents(components, enabled);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#disable(org.eclipse.pde.api.tools.internal.provisional.IApiComponent)
	 */
	public void disable(IApiComponent component) {
		getUnderlyingProfile().disable(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#dispose()
	 */
	public void dispose() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#enable(org.eclipse.pde.api.tools.internal.provisional.IApiComponent)
	 */
	public void enable(IApiComponent component) {
		getUnderlyingProfile().enable(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getApiComponent(java.lang.String)
	 */
	public IApiComponent getApiComponent(String id) {
		return getUnderlyingProfile().getApiComponent(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getApiComponents()
	 */
	public IApiComponent[] getApiComponents() {
		return getUnderlyingProfile().getApiComponents();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getDependentComponents(org.eclipse.pde.api.tools.internal.provisional.IApiComponent[])
	 */
	public IApiComponent[] getDependentComponents(IApiComponent[] components) {
		return getUnderlyingProfile().getDependentComponents(components);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getExecutionEnvironment()
	 */
	public String getExecutionEnvironment() {
		return getUnderlyingProfile().getExecutionEnvironment();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getId()
	 */
	public String getId() {
		return getUnderlyingProfile().getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getName()
	 */
	public String getName() {
		return getUnderlyingProfile().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getPrerequisiteComponents(org.eclipse.pde.api.tools.internal.provisional.IApiComponent[])
	 */
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components) {
		return getUnderlyingProfile().getPrerequisiteComponents(components);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#getVersion()
	 */
	public String getVersion() {
		return getUnderlyingProfile().getVersion();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#isEnabled(org.eclipse.pde.api.tools.internal.provisional.IApiComponent)
	 */
	public boolean isEnabled(IApiComponent component) {
		return getUnderlyingProfile().isEnabled(component);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#newApiComponent(java.lang.String)
	 */
	public IApiComponent newApiComponent(String location) throws CoreException {
		return getUnderlyingProfile().newApiComponent(location);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#newApiComponent(org.eclipse.pde.core.plugin.IPluginModelBase)
	 */
	public IApiComponent newApiComponent(IPluginModelBase model) throws CoreException {
		return getUnderlyingProfile().newApiComponent(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#removeApiComponents(org.eclipse.pde.api.tools.internal.provisional.IApiComponent[])
	 */
	public void removeApiComponents(IApiComponent[] components) {
		getUnderlyingProfile().removeApiComponents(components);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#resolvePackage(org.eclipse.pde.api.tools.internal.provisional.IApiComponent, java.lang.String)
	 */
	public IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException {
		return getUnderlyingProfile().resolvePackage(sourceComponent, packageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#setEnabled(org.eclipse.pde.api.tools.internal.provisional.IApiComponent[])
	 */
	public void setEnabled(IApiComponent[] components) {
		getUnderlyingProfile().setEnabled(components);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#setExecutionEnvironment(java.io.File)
	 */
	public void setExecutionEnvironment(File eefile) throws CoreException {
		getUnderlyingProfile().setExecutionEnvironment(eefile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#setId(java.lang.String)
	 */
	public void setId(String id) {
		getUnderlyingProfile().setId(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#setName(java.lang.String)
	 */
	public void setName(String name) {
		getUnderlyingProfile().setName(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#setVersion(java.lang.String)
	 */
	public void setVersion(String version) {
		getUnderlyingProfile().setVersion(version);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfile#writeProfileDescription(java.io.OutputStream)
	 */
	public void writeProfileDescription(OutputStream stream) throws CoreException {
		getUnderlyingProfile().writeProfileDescription(stream);
	}

}
