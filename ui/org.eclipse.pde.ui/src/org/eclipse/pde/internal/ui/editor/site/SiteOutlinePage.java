/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.Composite;

public class SiteOutlinePage extends FormOutlinePage {
	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof SitePage) {
				return getCategoryDefs();
			}
			if (parent instanceof FeaturePage) {
				return getFeatures();
			}
			if (parent instanceof ArchivePage) {
				return getArchives();
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

	public SiteOutlinePage(PDEFormPage formPage) {
		super(formPage);
		/*Image folderImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);*/
	}
	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}
	public void createControl(Composite parent) {
		super.createControl(parent);
		ISiteModel model = (ISiteModel) formPage.getModel();
		model.addModelChangedListener(this);
	}
	protected ILabelProvider createLabelProvider() {
		return PDEPlugin.getDefault().getLabelProvider();
	}
	public void dispose() {
		super.dispose();
		ISiteModel model = (ISiteModel) formPage.getModel();
		model.removeModelChangedListener(this);
	}

	public IPDEEditorPage getParentPage(Object item) {
		if (item instanceof ISiteFeature)
			return formPage.getEditor().getPage(SiteEditor.FEATURE_PAGE);
		if (item instanceof ISiteCategoryDefinition)
		return formPage.getEditor().getPage(SiteEditor.SITE_PAGE);
		if (item instanceof ISiteArchive)
			return formPage.getEditor().getPage(SiteEditor.ARCHIVE_PAGE);
		return super.getParentPage(item);
	}
	private Object[] getCategoryDefs() {
		ISiteModel model = (ISiteModel) formPage.getModel();
		ISite site = model.getSite();
		return site.getCategoryDefinitions();
	}
	private Object[] getFeatures() {
		ISiteModel model = (ISiteModel) formPage.getModel();
		ISite site = model.getSite();
		return site.getFeatures();
	}
	private Object[] getArchives() {
		ISiteModel model = (ISiteModel) formPage.getModel();
		ISite site = model.getSite();
		return site.getArchives();
	}

	public Object getParent(Object object) {
		return getParentPage(object);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			treeViewer.refresh();
			return;
		}
		treeViewer.refresh();
		treeViewer.expandAll();
/*
		Object object = event.getChangedObjects()[0];
		if (object instanceof ISiteFeature) {
			if (event.getChangeType()== IModelChangedEvent.CHANGE) {
				String property = event.getChangedProperty();
				if (property.equals(IFeature.P_DESCRIPTION) ||
					property.equals(IFeature.P_COPYRIGHT) ||
					property.equals(IFeature.P_LICENSE)) {
					IPDEEditorPage page = formPage.getEditor().getPage(SiteEditor.INFO_PAGE);
					treeViewer.refresh(page);
					return;
				}
			}
		}
		if (object instanceof IFeatureImport
			|| object instanceof IFeatureInfo
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
*/
	}
}