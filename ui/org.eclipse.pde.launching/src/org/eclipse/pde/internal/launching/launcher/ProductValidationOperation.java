/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM Corporation - ongoing enhancements
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

	@Override
	protected IPluginModelBase[] getModels() throws CoreException {
		return fModels;
	}

	@Override
	protected IExecutionEnvironment[] getMatchingEnvironments() throws CoreException {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();

		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		List<IExecutionEnvironment> result = new ArrayList<>(envs.length);
		for (IExecutionEnvironment env : envs) {
			IVMInstall[] compatible = env.getCompatibleVMs();
			for (IVMInstall element : compatible) {
				if (element.equals(install)) {
					result.add(env);
					break;
				}
			}
		}
		return result.toArray(new IExecutionEnvironment[result.size()]);
	}

}
