/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;
import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;

public class FeatureOutlinePage extends FormOutlinePage {
	private static final String KEY_REFERENCED_PLUGINS = "FeatureEditor.Outline.referencedPlugins"; //$NON-NLS-1$
	private static final String KEY_REQUIRED_PLUGINS = "FeatureEditor.Outline.requiredPlugins"; //$NON-NLS-1$
	private NamedElement referencedPlugins, requiredPlugins;
	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
			if (model.isValid()) {
				if (parent instanceof FeatureFormPage) {
					return getURLs();
				}
				if (parent instanceof InfoFormPage) {
					return getInfos();
				}
				if (parent instanceof FeatureReferencePage) {
					return new Object[]{referencedPlugins, requiredPlugins};
				}
				if (parent.equals(requiredPlugins)) {
					return getImports();
				}
				if (parent.equals(referencedPlugins)) {
					return getReferences();
				}
			}
			return super.getChildren(parent);
		}
		public Object getParent(Object child) {
			String pageId = getParentPageId(child);
			if (pageId != null)
				return editor.findPage(pageId);
			return super.getParent(child);
		}
	}
	public FeatureOutlinePage(PDEFormEditor editor) {
		super(editor);
		Image folderImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FOLDER);
		requiredPlugins = new NamedElement(PDEPlugin
				.getResourceString(KEY_REQUIRED_PLUGINS), folderImage);
		referencedPlugins = new NamedElement(PDEPlugin
				.getResourceString(KEY_REFERENCED_PLUGINS), folderImage);
	}
	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		model.addModelChangedListener(this);
	}
	public void dispose() {
		super.dispose();
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		model.removeModelChangedListener(this);
	}
	public String getParentPageId(Object item) {
		if (item instanceof IFeatureURLElement)
			return FeatureFormPage.PAGE_ID;
		if (item.equals(requiredPlugins) || item.equals(referencedPlugins)
				|| item instanceof IFeaturePlugin
				|| item instanceof IFeatureImport)
			return FeatureReferencePage.PAGE_ID;
		if (item instanceof IFeatureInfo)
			return InfoFormPage.PAGE_ID;
		if (item instanceof IFeatureData)
			return FeatureAdvancedPage.PAGE_ID;
		return super.getParentPageId(item);
	}
	private Object[] getInfos() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
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
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		return feature.getPlugins();
	}
	private Object[] getImports() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		return feature.getImports();
	}
	private Object[] getURLs() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		IFeatureURL url = feature.getURL();
		if (url == null)
			return new Object[0];
		IFeatureURLElement[] updates = url.getUpdates();
		IFeatureURLElement[] discoveries = url.getDiscoveries();
		int size = updates.length + discoveries.length;
		Object[] result = new Object[size];
		System.arraycopy(updates, 0, result, 0, updates.length);
		System.arraycopy(discoveries, 0, result, updates.length,
				discoveries.length);
		return result;
	}
	public Object getParent(Object object) {
		if (object instanceof IFeaturePlugin)
			return referencedPlugins;
		if (object instanceof IFeatureImport)
			return requiredPlugins;
		return editor.findPage(getParentPageId(object));
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			treeViewer.refresh();
			return;
		}
		Object object = event.getChangedObjects()[0];
		if (object instanceof IFeature) {
			if (event.getChangeType() == IModelChangedEvent.CHANGE) {
				String property = event.getChangedProperty();
				if (property.equals(IFeature.P_DESCRIPTION)
						|| property.equals(IFeature.P_COPYRIGHT)
						|| property.equals(IFeature.P_LICENSE)) {
					IFormPage page = editor.findPage(InfoFormPage.PAGE_ID);
					treeViewer.refresh(page);
					return;
				}
			}
		}
		if (object instanceof IFeatureImport || object instanceof IFeatureInfo
				|| object instanceof IFeaturePlugin
				|| object instanceof IFeatureData
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
