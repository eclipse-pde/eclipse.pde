/*
 * Created on Mar 17, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.neweditor.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
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
			PDEFormPage page = (PDEFormPage)parent;
			IPluginModelBase model = (IPluginModelBase)page.getModel();
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
		return new Object[0];
	}
	protected String getParentPageId(Object item) {
		String pageId=null;
		if (item instanceof IPluginImport)
			pageId = DependenciesPage.PAGE_ID;
		else if (item instanceof IPluginLibrary)
			pageId = RuntimePage.PAGE_ID;
		else if (item instanceof IPluginExtension)
			pageId = ExtensionsPage.PAGE_ID;
		else if (item instanceof IPluginExtensionPoint)
			pageId = ExtensionPointsPage.PAGE_ID;
		if (pageId!=null)
			return pageId;
		return super.getParentPageId(item);
	}
}