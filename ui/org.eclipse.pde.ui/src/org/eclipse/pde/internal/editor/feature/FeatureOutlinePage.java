package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.editor.*;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ISharedImages;

public class FeatureOutlinePage extends FormOutlinePage {
	private static final String KEY_REFERENCED_PLUGINS =
		"FeatureEditor.Outline.referencedPlugins";
	private static final String KEY_REQUIRED_PLUGINS =
		"FeatureEditor.Outline.requiredPlugins";
	private Image urlImage;
	private Image pluginImage;
	private Image pluginReqImage;
	private Image fragmentImage;
	private Image requiredPluginImage;
	private Image infoImage;
	private FolderObject referencedPlugins, requiredPlugins;

	class FolderObject {
		private Image image;
		private String key;
		public FolderObject(String key, Image image) {
			this.key = key;
			this.image = image;
		}
		public Image getImage() {
			return image;
		}
		public String toString() {
			return PDEPlugin.getResourceString(key);
		}
	}

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof FeatureFormPage) {
				return getURLs();
			}
			if (parent instanceof InfoFormPage) {
				return getInfos();
			}
			if (parent instanceof FeatureReferencePage) {
				return new Object[] { referencedPlugins, requiredPlugins };
			}
			if (parent.equals(requiredPlugins)) {
				return getImports();
			}
			if (parent.equals(referencedPlugins)) {
				return getReferences();
			}
			return super.getChildren(parent);
		}
		public Object getParent(Object child) {
			Object parent = getParentPage(child);
			if (parent != null)
				return parent;
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
			Image image = getObjectImage(obj);
			if (image != null)
				return image;
			return super.getImage(obj);
		}
	}

	public FeatureOutlinePage(PDEFormPage formPage) {
		super(formPage);
		urlImage = PDEPluginImages.DESC_LINK_OBJ.createImage();
		pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
		fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
		pluginReqImage = PDEPluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
		infoImage = PDEPluginImages.DESC_DOC_SECTION_OBJ.createImage();
		Image folderImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);
		requiredPlugins = new FolderObject(KEY_REQUIRED_PLUGINS, folderImage);
		referencedPlugins = new FolderObject(KEY_REFERENCED_PLUGINS, folderImage);
	}
	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		model.addModelChangedListener(this);
	}
	protected ILabelProvider createLabelProvider() {
		return new OutlineLabelProvider();
	}
	public void dispose() {
		super.dispose();
		urlImage.dispose();
		pluginReqImage.dispose();
		infoImage.dispose();
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		model.removeModelChangedListener(this);
	}
	Image getObjectImage(Object obj) {
		if (obj instanceof IFeatureURLElement) {
			return urlImage;
		}
		if (obj instanceof IFeaturePlugin) {
			IFeaturePlugin fref = (IFeaturePlugin) obj;
			if (fref.isFragment())
				return fragmentImage;
			else
				return pluginImage;
		}
		if (obj instanceof IFeatureImport) {
			IFeatureImport iimport = (IFeatureImport) obj;
			return pluginReqImage;
		}
		if (obj instanceof FolderObject) {
			return ((FolderObject) obj).getImage();
		}
		if (obj instanceof IFeatureInfo) {
			return infoImage;
		}
		return null;
	}
	String getObjectLabel(Object obj) {
		if (obj instanceof IFeatureURLElement) {
			return ((IFeatureURLElement) obj).getLabel();
		}
		if (obj instanceof IFeaturePlugin) {
			return ((IFeaturePlugin) obj).getLabel();
		}
		if (obj instanceof FolderObject) {
			return obj.toString();
		}
		if (obj instanceof IFeatureInfo) {
			return obj.toString();
		}
		return null;
	}
	public IPDEEditorPage getParentPage(Object item) {
		if (item instanceof IFeatureURLElement)
			return formPage.getEditor().getPage(FeatureEditor.FEATURE_PAGE);
		if (item.equals(requiredPlugins)
			|| item.equals(referencedPlugins)
			|| item instanceof IFeaturePlugin
			|| item instanceof IFeatureImport)
			return formPage.getEditor().getPage(FeatureEditor.REFERENCE_PAGE);
		if (item instanceof IFeatureInfo)
			return formPage.getEditor().getPage(FeatureEditor.INFO_PAGE);
		return super.getParentPage(item);
	}
	private Object[] getInfos() {
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		IFeature feature = model.getFeature();
		Vector result = new Vector();
		for (int i = 0; i < 3; i++) {
			IFeatureInfo info = feature.getFeatureInfo(i);
			if (info != null)
				result.add(info);
		}
		return result.toArray();
	}
	private Object[] getReferences() {
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		IFeature feature = model.getFeature();
		return feature.getPlugins();
	}
	private Object[] getImports() {
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		IFeature feature = model.getFeature();
		return feature.getImports();
	}
	private Object[] getURLs() {
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		IFeature component = model.getFeature();
		IFeatureURL url = component.getURL();
		if (url == null)
			return new Object[0];
		IFeatureURLElement[] updates = url.getUpdates();
		IFeatureURLElement[] discoveries = url.getDiscoveries();
		int size = updates.length + discoveries.length;
		Object[] result = new Object[size];
		System.arraycopy(updates, 0, result, 0, updates.length);
		System.arraycopy(discoveries, 0, result, updates.length, discoveries.length);
		return result;
	}

	public Object getParent(Object object) {
		if (object instanceof IFeaturePlugin)
			return referencedPlugins;
		if (object instanceof IFeatureImport)
			return requiredPlugins;
		return getParentPage(object);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			treeViewer.refresh();
			return;
		}
		Object object = event.getChangedObjects()[0];
		if (object instanceof IFeature) {
			if (event.getChangeType()== IModelChangedEvent.CHANGE) {
				String property = event.getChangedProperty();
				if (property.equals(IFeature.P_DESCRIPTION) ||
					property.equals(IFeature.P_COPYRIGHT) ||
					property.equals(IFeature.P_LICENSE)) {
					IPDEEditorPage page = formPage.getEditor().getPage(FeatureEditor.INFO_PAGE);
					treeViewer.update(page, null);
					return;
				}
			}
		}
		if (object instanceof IFeatureImport
			|| object instanceof IFeatureInfo
			|| object instanceof IFeaturePlugin
			|| object instanceof IFeatureURLElement) {
			if (event.getChangeType() == IModelChangedEvent.CHANGE) {
				treeViewer.update(object, null);
			} else {
				// find the parent
				Object parent = null;

				parent = getParent(object);
				if (parent != null) {
					if (event.getChangeType() == IModelChangedEvent.INSERT)
						treeViewer.add(parent, event.getChangedObjects());
					else
						treeViewer.remove(event.getChangedObjects());
				} else {
					treeViewer.refresh();
					treeViewer.expandAll();
				}
			}
		}
	}
}