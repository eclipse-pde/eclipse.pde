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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;

/**
 * @version 	1.0
 * @author
 */
public class FeatureUndoManager extends ModelUndoManager {
	IFeatureModel model;

	public FeatureUndoManager(PDEMultiPageEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	/*
	 * @see IModelUndoManager#execute(ModelUndoOperation)
	 */

	public void connect(IModelChangeProvider provider) {
		model = (IFeatureModel) provider;
		super.connect(provider);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof IFeature || obj instanceof IFeatureURL)
			return FeatureEditor.FEATURE_PAGE;
		if (obj instanceof IFeaturePlugin || obj instanceof IFeatureImport)
			return FeatureEditor.REFERENCE_PAGE;
		if (obj instanceof IFeatureData || obj instanceof IFeatureChild)
			return FeatureEditor.ADVANCED_PAGE;
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
		IFeature feature = model.getFeature();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IFeaturePlugin) {
					feature.addPlugins(new IFeaturePlugin [] {(IFeaturePlugin) element});
				} else if (element instanceof IFeatureData) {
					feature.addData(new IFeatureData [] {(IFeatureData) element});
				} else if (element instanceof IFeatureImport) {
					feature.addImports(new IFeatureImport [] {(IFeatureImport) element});
				} else if (element instanceof IFeatureChild) {
					feature.addIncludedFeatures(new IFeatureChild [] {(IFeatureChild) element});
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeRemove(Object[] elements) {
		IFeature feature = model.getFeature();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IFeaturePlugin) {
					feature.removePlugins(new IFeaturePlugin [] {(IFeaturePlugin) element});
				} else if (element instanceof IFeatureData) {
					feature.removeData(new IFeatureData [] {(IFeatureData) element});
				} else if (element instanceof IFeatureImport) {
					feature.removeImports(new IFeatureImport [] {(IFeatureImport) element});
				} else if (element instanceof IFeatureChild) {
					feature.removeIncludedFeatures(new IFeatureChild [] {(IFeatureChild) element});
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
			IFeatureObject obj = (IFeatureObject) event.getChangedObjects()[0];
			//Ignore events from objects that are not yet in the model.
			if (!(obj instanceof IFeature) && obj.isInTheModel() == false)
				return;
		}
		super.modelChanged(event);
	}
}
