/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;

public interface IClasspathComputer {
	public List<Object> getClasspath(BundleDescription model, ModelBuildScriptGenerator.CompiledEntry jar) throws CoreException;
}
