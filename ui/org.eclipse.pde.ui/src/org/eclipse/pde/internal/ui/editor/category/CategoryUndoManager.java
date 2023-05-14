/*******************************************************************************
 * Copyright (c) 2013, 2016 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *   IBM Corporation - ongoing enhancements
 ******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.core.site.SiteObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelUndoManager;

public class CategoryUndoManager extends ModelUndoManager {
	public CategoryUndoManager(CategoryEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	@Override
	protected String getPageId(Object obj) {
		if (obj instanceof ISiteFeature || obj instanceof ISiteBundle || obj instanceof ISiteCategory || obj instanceof ISiteCategoryDefinition) {
			return IUsPage.PAGE_ID;
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
				} else if (element instanceof ISiteBundle) {
					site.addBundles(new ISiteBundle[] {(ISiteBundle) element});
				} else if (element instanceof ISiteArchive) {
					site.addArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.addCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteObject siteObject = category.getParent();
					if (siteObject instanceof ISiteFeature) {
						((ISiteFeature) siteObject).addCategories(new ISiteCategory[] {category});
					} else if (siteObject instanceof ISiteBundle) {
						((ISiteBundle) siteObject).addCategories(new ISiteCategory[] {category});
					}
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
				} else if (element instanceof ISiteBundle) {
					site.removeBundles(new ISiteBundle[] {(ISiteBundle) element});
				} else if (element instanceof ISiteArchive) {
					site.removeArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.removeCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteObject siteObject = category.getParent();
					if (siteObject instanceof ISiteFeature) {
						((ISiteFeature) siteObject).removeCategories(new ISiteCategory[] {category});
					} else if (siteObject instanceof ISiteBundle) {
						((ISiteBundle) siteObject).removeCategories(new ISiteCategory[] {category});
					}

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
