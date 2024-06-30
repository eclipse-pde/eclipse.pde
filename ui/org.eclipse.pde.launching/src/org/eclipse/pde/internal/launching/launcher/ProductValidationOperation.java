/*******************************************************************************
 * Copyright (c) 2009, 2022 EclipseSource Corporation and others.
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

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ProductValidationOperation extends LaunchValidationOperation {

	public ProductValidationOperation(Set<IPluginModelBase> models) {
		super(null, models, null);
	}

	@Override
	protected IExecutionEnvironment[] getMatchingEnvironments() throws CoreException {
		return LaunchValidationOperation.getMatchingEEs(JavaRuntime.getDefaultVMInstall());
	}

}
