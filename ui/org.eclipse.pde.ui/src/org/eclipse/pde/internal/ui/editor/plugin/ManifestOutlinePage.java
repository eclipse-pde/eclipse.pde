/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Mar 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ManifestOutlinePage extends FormOutlinePage {
	/**
	 * @param editor
	 */
	public ManifestOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage) {
			PDEFormPage page = (PDEFormPage) parent;
			IPluginModelBase model = (IPluginModelBase) page.getModel();
			IBuildModel buildModel = getBuildModel();
			if (model.isValid()) {
				IPluginBase pluginBase = model.getPluginBase();
				if (page.getId().equals(DependenciesPage.PAGE_ID))
					return pluginBase.getImports();
				if (page.getId().equals(RuntimePage.PAGE_ID))
					return pluginBase.getLibraries();
				if (page.getId().equals(ExtensionsPage.PAGE_ID))
					return pluginBase.getExtensions();
				if (page.getId().equals(ExtensionPointsPage.PAGE_ID))
					return pluginBase.getExtensionPoints();
			}
			if (buildModel!=null && buildModel.isValid()) {
				if (page.getId().equals(BuildPage.PAGE_ID))
					return buildModel.getBuild().getBuildEntries();
			}
		}
		return new Object[0];
	}
	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IPluginImport)
			pageId = DependenciesPage.PAGE_ID;
		else if (item instanceof IPluginLibrary)
			pageId = RuntimePage.PAGE_ID;
		else if (item instanceof IPluginExtension)
			pageId = ExtensionsPage.PAGE_ID;
		else if (item instanceof IPluginExtensionPoint)
			pageId = ExtensionPointsPage.PAGE_ID;
		else if (item instanceof IBuildEntry)
			pageId = BuildPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}
	private IBuildModel getBuildModel() {
		InputContext context = editor.getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context!=null)
			return (IBuildModel)context.getModel();
		return null;
	}
}