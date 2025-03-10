/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageFriend;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;

public class ManifestOutlinePage extends FormOutlinePage {
	public ManifestOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage page) {
			if (page.getModel() instanceof IPluginModelBase model) {
				if (model.isValid()) {
					IPluginBase pluginBase = model.getPluginBase();
					if (page.getId().equals(DependenciesPage.PAGE_ID)) {
						return pluginBase.getImports();
					}
					if (page.getId().equals(RuntimePage.PAGE_ID)) {
						return pluginBase.getLibraries();
					}
					if (page.getId().equals(ExtensionsPage.PAGE_ID)) {
						return pluginBase.getExtensions();
					}
					if (page.getId().equals(ExtensionPointsPage.PAGE_ID)) {
						return pluginBase.getExtensionPoints();
					}
				}
			}
		}
		return new Object[0];
	}

	@Override
	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IPluginImport || item instanceof ImportPackageObject) {
			pageId = DependenciesPage.PAGE_ID;
		} else if (item instanceof IPluginLibrary || item instanceof ExportPackageObject || item instanceof PackageFriend) {
			pageId = RuntimePage.PAGE_ID;
		} else if (item instanceof IPluginExtension) {
			pageId = ExtensionsPage.PAGE_ID;
		} else if (item instanceof IPluginExtensionPoint) {
			pageId = ExtensionPointsPage.PAGE_ID;
		} else if (item instanceof IBuildEntry) {
			pageId = BuildPage.PAGE_ID;
		}
		if (pageId != null) {
			return pageId;
		}
		return super.getParentPageId(item);
	}
}
