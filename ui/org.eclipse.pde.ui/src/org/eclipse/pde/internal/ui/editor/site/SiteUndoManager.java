/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.SiteObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;

/**
 * @version 	1.0
 * @author
 */
public class SiteUndoManager extends ModelUndoManager {
	ISiteModel model;

	public SiteUndoManager(PDEMultiPageEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	/*
	 * @see IModelUndoManager#execute(ModelUndoOperation)
	 */

	public void connect(IModelChangeProvider provider) {
		model = (ISiteModel) provider;
		super.connect(provider);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof ISiteBuildFeature)
			return SiteEditor.BUILD_PAGE;
		if (obj instanceof ISiteFeature)
			return SiteEditor.FEATURE_PAGE;
		if (obj instanceof ISiteArchive)
			return SiteEditor.ARCHIVE_PAGE;
		if (obj instanceof ISiteCategory)
			return SiteEditor.FEATURE_PAGE;
		if (obj instanceof ISiteCategoryDefinition)
			return SiteEditor.SITE_PAGE;
		return null;
	}

	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();

		switch (type) {
			case IModelChangedEvent.INSERT :
				if (undo)
					executeRemove(elements);
				else
					executeAdd(elements);
				break;
			case IModelChangedEvent.REMOVE :
				if (undo)
					executeAdd(elements);
				else
					executeRemove(elements);
				break;
			case IModelChangedEvent.CHANGE :
				if (undo)
					executeChange(
						elements[0],
						propertyName,
						event.getNewValue(),
						event.getOldValue());
				else
					executeChange(
						elements[0],
						propertyName,
						event.getOldValue(),
						event.getNewValue());
		}
	}

	private void executeAdd(Object[] elements) {
		ISite site = model.getSite();
		ISiteBuild siteBuild = model.getBuildModel().getSiteBuild();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof ISiteFeature) {
					site.addFeatures(new ISiteFeature [] {(ISiteFeature) element});
				} else if (element instanceof ISiteBuildFeature) {
					siteBuild.addFeatures(new ISiteBuildFeature [] {(ISiteBuildFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.addArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.addCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory)element;
					ISiteFeature feature = (ISiteFeature)category.getParent();
					feature.addCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeRemove(Object[] elements) {
		ISite site = model.getSite();
		ISiteBuild siteBuild = model.getBuildModel().getSiteBuild();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof ISiteFeature) {
					site.removeFeatures(new ISiteFeature [] {(ISiteFeature) element});
				} else if (element instanceof ISiteBuildFeature) {
					siteBuild.removeFeatures(new ISiteBuildFeature [] {(ISiteBuildFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.removeArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.removeCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory)element;
					ISiteFeature feature = (ISiteFeature)category.getParent();
					feature.removeCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeChange(
		Object element,
		String propertyName,
		Object oldValue,
		Object newValue) {

		if (element instanceof SiteObject) {
			SiteObject sobj = (SiteObject) element;
			try {
				sobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			ISiteObject obj = (ISiteObject) event.getChangedObjects()[0];
			//Ignore events from objects that are not yet in the model.
			if (!(obj instanceof ISite) && obj.isInTheModel() == false)
				return;
		}
		super.modelChanged(event);
	}
}