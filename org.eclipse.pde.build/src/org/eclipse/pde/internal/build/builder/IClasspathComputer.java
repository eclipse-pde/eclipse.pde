/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;

public interface IClasspathComputer {
	public List getClasspath(BundleDescription model, ModelBuildScriptGenerator.CompiledEntry jar) throws CoreException;
}
