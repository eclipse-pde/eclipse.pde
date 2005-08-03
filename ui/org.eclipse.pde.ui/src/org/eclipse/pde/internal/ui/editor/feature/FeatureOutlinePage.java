/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.Vector;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureURL;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

public class FeatureOutlinePage extends FormOutlinePage {
	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
			if (model.isValid()) {
				if (parent instanceof FeatureFormPage) {
					return new Object[0];
				}
				if (parent instanceof InfoFormPage) {
					return getInfos();
				}
				if (parent.equals(fDiscoveryUrls)) {
					return getURLs();
				}
				if (parent instanceof FeatureReferencePage) {
					return getReferences();
				}
				if (parent instanceof FeatureIncludesPage) {
					return getIncludes();
				}
				if (parent instanceof FeatureDependenciesPage) {
					return getImports();
				}
				if (parent instanceof FeatureAdvancedPage) {
					return getData();
				}
			}
			if (parent instanceof PDEFormPage) {
				PDEFormPage page = (PDEFormPage) parent;
				IBuildModel buildModel = getBuildModel();
				if (buildModel != null && buildModel.isValid()) {
					if (page.getId().equals(BuildPage.PAGE_ID))
						return buildModel.getBuild().getBuildEntries();
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

	private NamedElement fDiscoveryUrls;

	public FeatureOutlinePage(PDEFormEditor editor) {
		super(editor);
		Image folderImage = PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_DOC_SECTION_OBJ);
		fDiscoveryUrls = new NamedElement(PDEUIMessages.FeatureOutlinePage_discoverUrls, 
				folderImage);
	}

	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}

	public String getParentPageId(Object item) {
		if (item instanceof IFeaturePlugin)
			return FeatureReferencePage.PAGE_ID;
		if (item instanceof IFeatureChild)
			return FeatureIncludesPage.PAGE_ID;
		if (item instanceof IFeatureImport)
			return FeatureDependenciesPage.PAGE_ID;
		if (item instanceof IFeatureInfo || item.equals(fDiscoveryUrls)
				|| item instanceof IFeatureURLElement)
			return InfoFormPage.PAGE_ID;
		if (item instanceof IFeatureData)
			return FeatureAdvancedPage.PAGE_ID;
		else if (item instanceof IBuildEntry)
			return BuildPage.PAGE_ID;
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
		result.add(fDiscoveryUrls);
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

	private Object[] getIncludes() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		return feature.getIncludedFeatures();
	}

	private Object[] getData() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		return feature.getData();
	}

	private Object[] getURLs() {
		IFeatureModel model = (IFeatureModel) editor.getAggregateModel();
		IFeature feature = model.getFeature();
		IFeatureURL url = feature.getURL();
		if (url == null)
			return new Object[0];
		return url.getDiscoveries();
	}

	public Object getParent(Object object) {
		if (object instanceof IFeatureURLElement) {
			return fDiscoveryUrls;
		}
		return editor.findPage(getParentPageId(object));
	}

	public void modelChanged(IModelChangedEvent event) {
		if (treeViewer.getControl().isDisposed()) {
			return;
		}
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

	private IBuildModel getBuildModel() {
		InputContext context = editor.getContextManager().findContext(
				BuildInputContext.CONTEXT_ID);
		if (context != null)
			return (IBuildModel) context.getModel();
		return null;
	}
}
