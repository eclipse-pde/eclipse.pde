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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An API component for a system library.
 * 
 * @since 1.0.0
 */
public class StubApiComponent extends SystemLibraryApiComponent {
	public static Map AllSystemLibraryApiComponents;

	public static IApiComponent getStubApiComponent(int eeValue) {
		if (!Platform.isRunning()) {
			return null;
		}
		if (AllSystemLibraryApiComponents == null) {
			AllSystemLibraryApiComponents = new HashMap();
		}
		String name = ProfileModifiers.getName(eeValue);
		IApiComponent component = (IApiComponent) AllSystemLibraryApiComponents.get(name);
		if (component == null) {
			IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(name);
			if (environment != null) {
				IVMInstall defaultVm = environment.getDefaultVM();
				if (defaultVm != null) {
					try {
						File eeFile = Util.createEEFile(defaultVm, name);
						ExecutionEnvironmentDescription ee = new ExecutionEnvironmentDescription(eeFile);
						component = new StubApiComponent(ApiBaselineManager.getManager().getWorkspaceBaseline(), ee, name);
						AllSystemLibraryApiComponents.put(name, component);
					} catch (IOException e) {
						ApiPlugin.log(e);
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
			}
		}
		return component;
	}

	/**
	 * Constructs a system library from the given execution environment description file.
	 * 
	 * @param profile owning profile
	 * @param fileName the file name that corresponds to the stub file for the corresponding profile
	 * @param profileName the given profile name
	 * @exception CoreException if unable to read the execution environment description file
	 */
	private StubApiComponent(IApiBaseline profile, ExecutionEnvironmentDescription environmentDescription, String name) throws CoreException {
		super(profile, environmentDescription, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiComponent#createApiFilterStore()
	 */
	protected IApiFilterStore createApiFilterStore() {
		//TODO
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#isSystemComponent()
	 */
	public boolean isSystemComponent() {
		return false;
	}
	public static void disposeAllCaches() {
		if (AllSystemLibraryApiComponents != null) {
			for (Iterator iterator = AllSystemLibraryApiComponents.values().iterator(); iterator.hasNext(); ) {
				IApiComponent apiComponent = (IApiComponent) iterator.next();
				apiComponent.dispose();
			}
		}
	}
}
