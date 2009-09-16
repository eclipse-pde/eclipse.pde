/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ProductValidationOperation extends LaunchValidationOperation {

	private IPluginModelBase[] fModels;

	public ProductValidationOperation(IPluginModelBase[] models) {
		super(null);
		fModels = models;
	}

	protected IPluginModelBase[] getModels() throws CoreException {
		return fModels;
	}

	protected IExecutionEnvironment[] getMatchingEnvironments() throws CoreException {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();

		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		List result = new ArrayList(envs.length);
		for (int i = 0; i < envs.length; i++) {
			IExecutionEnvironment env = envs[i];
			IVMInstall[] compatible = env.getCompatibleVMs();
			for (int j = 0; j < compatible.length; j++) {
				if (compatible[j].equals(install)) {
					result.add(env);
					break;
				}
			}
		}
		return (IExecutionEnvironment[]) result.toArray(new IExecutionEnvironment[result.size()]);
	}

}
