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
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;

/**
 * @version 	1.0
 * @author
 */
public class PluginUndoManager extends ModelUndoManager {
	
	public PluginUndoManager(PDEFormEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof IPluginBase)
			return OverviewPage.PAGE_ID;
		if (obj instanceof IPluginImport)
			return DependenciesPage.PAGE_ID;
		if (obj instanceof IPluginLibrary)
			return RuntimePage.PAGE_ID;
		if (obj instanceof IPluginExtension ||
			obj instanceof IPluginElement ||
			obj instanceof IPluginAttribute)
			return ExtensionsPage.PAGE_ID;
		if (obj instanceof IPluginExtensionPoint)
			return ExtensionPointsPage.PAGE_ID;
		return null;
	}
	
	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();
		IModelChangeProvider model = (IModelChangeProvider)event.getChangeProvider();

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
				if (event instanceof AttributeChangedEvent) {
					executeAttributeChange((AttributeChangedEvent) event, undo);
				} else {
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
	}

	private void executeAdd(IModelChangeProvider model, Object[] elements) {
		IPluginBase pluginBase=null;
		IBuild build=null;
		if (model instanceof IPluginModelBase)
			pluginBase = ((IPluginModelBase)model).getPluginBase();
		if (model instanceof IBuildModel)
			build = ((IBuildModel)model).getBuild();
		
		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IPluginImport) {
					pluginBase.add((IPluginImport) element);
				} else if (element instanceof IPluginLibrary) {
					pluginBase.add((IPluginLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.add((IPluginExtensionPoint) element);
				} else if (element instanceof IPluginExtension) {
					pluginBase.add((IPluginExtension) element);
				} else if (element instanceof IPluginElement) {
					IPluginElement e = (IPluginElement) element;
					IPluginParent p = (IPluginParent) e.getParent();
					p.add(e);
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry)element;
					build.add(e);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void executeRemove(IModelChangeProvider model, Object[] elements) {
		IPluginBase pluginBase=null;
		IBuild build=null;
		if (model instanceof IPluginModelBase)
			pluginBase = ((IPluginModelBase)model).getPluginBase();
		if (model instanceof IBuildModel)
			build = ((IBuildModel)model).getBuild();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IPluginImport) {
					pluginBase.remove((IPluginImport) element);
				} else if (element instanceof IPluginLibrary) {
					pluginBase.remove((IPluginLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.remove((IPluginExtensionPoint) element);
				} else if (element instanceof IPluginExtension) {
					pluginBase.remove((IPluginExtension) element);
				} else if (element instanceof IPluginElement) {
					IPluginElement e = (IPluginElement) element;
					IPluginParent p = (IPluginParent) e.getParent();
					p.remove(e);
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry)element;
					build.remove(e);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeAttributeChange(AttributeChangedEvent e, boolean undo) {
		PluginElement element = (PluginElement) e.getChangedObjects()[0];
		PluginAttribute att = (PluginAttribute) e.getChagedAttribute();
		Object oldValue = e.getOldValue();
		Object newValue = e.getNewValue();
		try {
			if (undo)
				element.setAttribute(att.getName(), oldValue.toString());
			else
				element.setAttribute(att.getName(), newValue.toString());
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}

	private void executeChange(
		Object element,
		String propertyName,
		Object oldValue,
		Object newValue) {
		if (element instanceof PluginObject) {
			PluginObject pobj = (PluginObject) element;
			try {
				pobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		else if (element instanceof BuildObject) {
			BuildObject bobj = (BuildObject) element;
			try {
				bobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object changedObject = event.getChangedObjects()[0];
			if (changedObject instanceof IPluginObject) {
				IPluginObject obj = (IPluginObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (!(obj instanceof IPluginBase) && obj.isInTheModel() == false)
					return;
			}
			if (changedObject instanceof IBuildObject) {
				IBuildObject obj = (IBuildObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (obj.isInTheModel() == false)
					return;
			}
		}
		super.modelChanged(event);
	}
}
