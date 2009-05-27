/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.feature.FeatureObject;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelUndoManager;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class FeatureUndoManager extends ModelUndoManager {

	public FeatureUndoManager(PDEFormEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof IFeature || obj instanceof IFeatureURL)
			return FeatureFormPage.PAGE_ID;
		if (obj instanceof IFeaturePlugin)
			return FeatureReferencePage.PAGE_ID;
		if (obj instanceof IFeatureImport)
			return FeatureDependenciesPage.PAGE_ID;
		if (obj instanceof IFeatureData || obj instanceof IFeatureChild)
			return FeatureAdvancedPage.PAGE_ID;
		return null;
	}

	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();
		IFeatureModel model = (IFeatureModel) event.getChangeProvider();

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

	private void executeAdd(IFeatureModel model, Object[] elements) {
		IFeature feature = model.getFeature();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IFeaturePlugin) {
					feature.addPlugins(new IFeaturePlugin[] {(IFeaturePlugin) element});
				} else if (element instanceof IFeatureData) {
					feature.addData(new IFeatureData[] {(IFeatureData) element});
				} else if (element instanceof IFeatureImport) {
					feature.addImports(new IFeatureImport[] {(IFeatureImport) element});
				} else if (element instanceof IFeatureChild) {
					feature.addIncludedFeatures(new IFeatureChild[] {(IFeatureChild) element});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeRemove(IFeatureModel model, Object[] elements) {
		IFeature feature = model.getFeature();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IFeaturePlugin) {
					feature.removePlugins(new IFeaturePlugin[] {(IFeaturePlugin) element});
				} else if (element instanceof IFeatureData) {
					feature.removeData(new IFeatureData[] {(IFeatureData) element});
				} else if (element instanceof IFeatureImport) {
					feature.removeImports(new IFeatureImport[] {(IFeatureImport) element});
				} else if (element instanceof IFeatureChild) {
					feature.removeIncludedFeatures(new IFeatureChild[] {(IFeatureChild) element});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {
		if (element instanceof FeatureObject) {
			FeatureObject pobj = (FeatureObject) element;
			try {
				pobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = event.getChangedObjects()[0];
			if (obj instanceof IFeatureObject) {
				IFeatureObject fobj = (IFeatureObject) event.getChangedObjects()[0];
				// Ignore events from objects that are not yet in the model.
				if (!(fobj instanceof IFeature) && fobj.isInTheModel() == false)
					return;
			}
		}
		super.modelChanged(event);
	}
}
