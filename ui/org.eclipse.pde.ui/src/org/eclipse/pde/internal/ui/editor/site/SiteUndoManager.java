/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.core.site.SiteObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelUndoManager;

public class SiteUndoManager extends ModelUndoManager {
	public SiteUndoManager(SiteEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	@Override
	protected String getPageId(Object obj) {
		if (obj instanceof ISiteDescription) {
			return ArchivePage.PAGE_ID;
		}
		if (obj instanceof ISiteFeature || obj instanceof ISiteCategory || obj instanceof ISiteCategoryDefinition) {
			return FeaturesPage.PAGE_ID;
		}
		// site elements and attributes are on different pages, stay on the
		// current page
		return null;
	}

	@Override
	protected void execute(IModelChangedEvent event, boolean undo) {
		IModelChangeProvider model = event.getChangeProvider();
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();

		switch (type) {
			case IModelChangedEvent.INSERT :
				if (undo)
					executeRemove(model, elements);
				else
					executeAdd(model, elements);
				break;
			case IModelChangedEvent.REMOVE :
				if (undo)
					executeAdd(model, elements);
				else
					executeRemove(model, elements);
				break;
			case IModelChangedEvent.CHANGE :
				if (undo)
					executeChange(elements[0], propertyName, event.getNewValue(), event.getOldValue());
				else
					executeChange(elements[0], propertyName, event.getOldValue(), event.getNewValue());
		}
	}

	private void executeAdd(IModelChangeProvider model, Object[] elements) {
		ISiteModel siteModel = (model instanceof ISiteModel) ? (ISiteModel) model : null;
		ISite site = siteModel != null ? siteModel.getSite() : null;

		try {
			for (Object element : elements) {
				if (element instanceof ISiteFeature) {
					site.addFeatures(new ISiteFeature[] {(ISiteFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.addArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.addCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteFeature feature = (ISiteFeature) category.getParent();
					feature.addCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeRemove(IModelChangeProvider model, Object[] elements) {
		ISiteModel siteModel = (model instanceof ISiteModel) ? (ISiteModel) model : null;
		ISite site = siteModel != null ? siteModel.getSite() : null;

		try {
			for (Object element : elements) {
				if (element instanceof ISiteFeature) {
					site.removeFeatures(new ISiteFeature[] {(ISiteFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.removeArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.removeCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteFeature feature = (ISiteFeature) category.getParent();
					feature.removeCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {

		if (element instanceof SiteObject) {
			SiteObject sobj = (SiteObject) element;
			try {
				sobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = event.getChangedObjects()[0];
			if (object instanceof ISiteObject) {
				ISiteObject obj = (ISiteObject) object;
				//Ignore events from objects that are not yet in the model.
				if (!(obj instanceof ISite) && !obj.isInTheModel())
					return;
			}
		}
		super.modelChanged(event);
	}
}
