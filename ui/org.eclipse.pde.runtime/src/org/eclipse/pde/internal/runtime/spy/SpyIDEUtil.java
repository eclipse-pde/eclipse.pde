/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Marcelo Paternostro <marcelop@ca.ibm.com> - bug 201105
 *     Kevin Doyle <kjdoyle@ca.ibm.com> - bug 208137
 *     Willian Mitsuda <wmitsuda@gmail.com> - bug 209841
 *******************************************************************************/

package org.eclipse.pde.internal.runtime.spy;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

/**
 * @since 3.4
 */
public class SpyIDEUtil {
	public static void openClass(String pluginId, String clazz) {
		IPluginModelBase model = PluginRegistry.findModel(pluginId);
		IResource resource = model != null ? model.getUnderlyingResource() : null;
		IJavaProject project = null;

		// if we don't find a model
		if (model == null) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), PDERuntimeMessages.SpyIDEUtil_noSourceFound_title, NLS.bind(PDERuntimeMessages.SpyIDEUtil_noSourceFound_message, new Object[] {clazz}));
			return;
		}

		if (resource != null) { // project is open in workspace
			project = JavaCore.create(resource.getProject());
		} else {
			SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
			try {
				manager.createProxyProject(new NullProgressMonitor());
				manager.addToJavaSearch(new IPluginModelBase[] {model});
				project = manager.getProxyProject();
			} catch (CoreException e) {
			}
		}
		if (project != null)
			openInEditor(project, clazz);
	}

	public static void openInEditor(IJavaProject project, String clazz) {
		try {
			IType type = project.findType(clazz);
			JavaUI.openInEditor(type, false, true);
		} catch (JavaModelException e) {
			PDERuntimePlugin.log(e);
		} catch (PartInitException e) {
			PDERuntimePlugin.log(e);
		}
	}

	public static void openBundleManifest(String bundleID) {
		ManifestEditor.openPluginEditor(bundleID);
	}

}
