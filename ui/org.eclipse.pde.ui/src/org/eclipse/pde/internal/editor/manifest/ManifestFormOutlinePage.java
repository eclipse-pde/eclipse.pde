package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.editor.*;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.model.*;

public class ManifestFormOutlinePage extends FormOutlinePage {
	private Vector topics;
	private Image overviewPageImage;
	private Image runtimePageImage;
	private Image dependenciesPageImage;
	private Image extensionsPageImage;
	private Image extensionPointsPageImage;

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof ManifestExtensionsPage) {
				return getPlugin().getExtensions();
			}
			if (parent instanceof ManifestExtensionPointPage) {
				return getPlugin().getExtensionPoints();
			}
			if (parent instanceof ManifestDependenciesPage) {
				IPluginBase base = getPlugin();
				if (base instanceof IPlugin)
					return ((IPlugin) base).getImports();
			}
			if (parent instanceof ManifestRuntimePage) {
				return getPlugin().getLibraries();
			}
			return super.getChildren(parent);
		}
		public Object getParent(Object child) {
			return super.getParent(child);
		}
	}

	class OutlineLabelProvider extends BasicLabelProvider {
		public String getText(Object obj) {
			String label = getObjectLabel(obj);
			if (label != null)
				return label;
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			Image image = provider.getImage(obj);
			if (image != null)
				return image;
			if (obj instanceof PDEFormPage) {
				image = getPageImage((PDEFormPage) obj);
				if (image != null)
					return image;
			}
			return super.getImage(obj);
		}
	}

	public ManifestFormOutlinePage(PDEFormPage formPage) {
		super(formPage);
		initializeImages();
	}

	private void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		overviewPageImage = provider.get(PDEPluginImages.DESC_OVERVIEW_OBJ);
		dependenciesPageImage = provider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
		extensionsPageImage = provider.get(PDEPluginImages.DESC_EXTENSIONS_OBJ);
		extensionPointsPageImage = provider.get(PDEPluginImages.DESC_EXT_POINTS_OBJ);
		runtimePageImage = provider.get(PDEPluginImages.DESC_RUNTIME_OBJ);
	}
	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		IPluginModelBase model = (IPluginModelBase) formPage.getModel();
		model.addModelChangedListener(this);
	}
	protected ILabelProvider createLabelProvider() {
		return new OutlineLabelProvider();
	}
	public void dispose() {
		super.dispose();
		IPluginModelBase model = (IPluginModelBase) formPage.getModel();
		model.removeModelChangedListener(this);
	}

	String getObjectLabel(Object obj) {
		boolean fullNames = MainPreferencePage.isFullNameModeEnabled();
		if (obj instanceof IPluginImport) {
			String pluginId = ((IPluginImport) obj).getId();
			if (!fullNames)
				return pluginId;
			IPlugin plugin = PDEPlugin.getDefault().findPlugin(pluginId);
			if (plugin != null)
				return plugin.getResourceString(plugin.getName());
			return pluginId;
		}
		if (obj instanceof IPluginLibrary) {
			return ((IPluginLibrary) obj).getName();
		}

		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames)
				return extension.getPoint();
			ISchema schema =
				PDEPlugin.getDefault().getSchemaRegistry().getSchema(extension.getPoint());

			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			// try extension point declaration
			IPluginExtensionPoint pointInfo =
				PDEPlugin.getDefault().getExternalModelManager().findExtensionPoint(
					extension.getPoint());
			if (pointInfo != null) {
				return pointInfo.getResourceString(pointInfo.getName());
			}
		}
		if (obj instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint point = (IPluginExtensionPoint) obj;
			if (!fullNames)
				return point.getId();
			return point.getTranslatedName();
		}
		return null;
	}
	private Image getPageImage(PDEFormPage page) {
		if (page instanceof ManifestFormPage) {
			return overviewPageImage;
		}
		if (page instanceof ManifestDependenciesPage) {
			return dependenciesPageImage;
		}
		if (page instanceof ManifestExtensionsPage) {
			return extensionsPageImage;
		}
		if (page instanceof ManifestExtensionPointPage) {
			return extensionPointsPageImage;
		}
		if (page instanceof ManifestRuntimePage) {
			return runtimePageImage;
		}
		return null;
	}
	public IPDEEditorPage getParentPage(Object item) {
		if (item instanceof IPluginImport)
			return formPage.getEditor().getPage(ManifestEditor.DEPENDENCIES_PAGE);
		if (item instanceof IPluginExtension)
			return formPage.getEditor().getPage(ManifestEditor.EXTENSIONS_PAGE);
		if (item instanceof IPluginExtensionPoint)
			return formPage.getEditor().getPage(ManifestEditor.EXTENSION_POINT_PAGE);
		if (item instanceof IPluginLibrary)
			return formPage.getEditor().getPage(ManifestEditor.RUNTIME_PAGE);
		return super.getParentPage(item);
	}
	IPluginBase getPlugin() {
		IPluginModelBase model = (IPluginModelBase) formPage.getModel();
		return model.getPluginBase();
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			treeViewer.refresh();
			treeViewer.expandAll();
			return;
		}
		Object object = event.getChangedObjects()[0];
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			treeViewer.update(object, null);
		} else {
			// find the parent
			Object parent = null;

			if (object instanceof IPluginExtension
				|| object instanceof IPluginExtensionPoint
				|| object instanceof IPluginLibrary
				|| object instanceof IPluginImport)
				parent = getParentPage(object);
			else if (object instanceof IPluginObject) {
				parent = ((IPluginObject) object).getParent();
			}
			if (parent != null) {
				//treeViewer.refresh(parent);
				//treeViewer.expandToLevel(parent, 2);
				if (event.getChangeType() == IModelChangedEvent.INSERT)
					treeViewer.add(parent, object);
				else
					treeViewer.remove(object);
			} else {
				treeViewer.refresh();
				treeViewer.expandAll();
			}
		}
	}
}