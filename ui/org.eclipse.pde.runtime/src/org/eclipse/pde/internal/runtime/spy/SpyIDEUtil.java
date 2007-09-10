/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Marcelo Paternostro <marcelop@ca.ibm.com> - bug 201105
 *******************************************************************************/

package org.eclipse.pde.internal.runtime.spy;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.ui.PartInitException;

public class SpyIDEUtil {
	public static void openClass(String pluginId, String clazz) {
		IPluginModelBase model = PluginRegistry.findModel(pluginId);
		IResource resource = model != null ? model.getUnderlyingResource()
				: null;
		IJavaProject project;
		if (resource != null) { // project is open in workspace
			project = JavaCore.create(resource.getProject());
		} else {
			SearchablePluginsManager manager = PDECore.getDefault()
					.getSearchablePluginsManager();
			project = manager.getProxyProject();
			manager.addToJavaSearch(new IPluginModelBase[] { model });
		}
		if (project != null)
			openInEditor(project, clazz);
	}

	public static void openInEditor(IJavaProject project, String clazz) {
		try {
			IType type = project.findType(clazz);
			JavaUI.openInEditor(type, false, true);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
	}
	
}
