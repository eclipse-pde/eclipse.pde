package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage.BasicContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;

public class FeatureOutlinePage extends FormOutlinePage {
	private static final String KEY_REFERENCED_PLUGINS =
		"FeatureEditor.Outline.referencedPlugins";
	private static final String KEY_REQUIRED_PLUGINS =
		"FeatureEditor.Outline.requiredPlugins";
	private NamedElement referencedPlugins, requiredPlugins;

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

	public FeatureOutlinePage(PDEFormPage formPage) {
		super(formPage);
		Image folderImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);
		requiredPlugins = new NamedElement(PDEPlugin.getResourceString(KEY_REQUIRED_PLUGINS), folderImage);
		referencedPlugins = new NamedElement(PDEPlugin.getResourceString(KEY_REFERENCED_PLUGINS), folderImage);
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
		return PDEPlugin.getDefault().getLabelProvider();
	}
	public void dispose() {
		super.dispose();
		IFeatureModel model = (IFeatureModel) formPage.getModel();
		model.removeModelChangedListener(this);
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
		IFeature feature = model.getFeature();
		IFeatureURL url = feature.getURL();
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
					treeViewer.refresh(page);
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