/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;

/**
 * @version 	1.0
 * @author
 */
public class PluginUndoManager extends ModelUndoManager {
	AbstractPluginModelBase model;
	
	public PluginUndoManager(PDEMultiPageEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	/*
	 * @see IModelUndoManager#execute(ModelUndoOperation)
	 */

	public void connect(IModelChangeProvider provider) {
		model = (AbstractPluginModelBase) provider;
		super.connect(provider);
	}
	
	protected String getPageId(Object obj) {
		if (obj instanceof IPluginBase)
			return ManifestEditor.OVERVIEW_PAGE;
		if (obj instanceof IPluginImport)
			return ManifestEditor.DEPENDENCIES_PAGE;
		if (obj instanceof IPluginLibrary)
			return ManifestEditor.RUNTIME_PAGE;
		if (obj instanceof IPluginExtension ||
			obj instanceof IPluginElement ||
			obj instanceof IPluginAttribute)
			return ManifestEditor.EXTENSIONS_PAGE;
		if (obj instanceof IPluginExtensionPoint)
			return ManifestEditor.EXTENSION_POINT_PAGE;
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

	private void executeAdd(Object[] elements) {
		IPluginBase pluginBase = model.getPluginBase();

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
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void executeRemove(Object[] elements) {
		IPluginBase pluginBase = model.getPluginBase();

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
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			IPluginObject obj = (IPluginObject) event.getChangedObjects()[0];
			//Ignore events from objects that are not yet in the model.
			if (!(obj instanceof IPluginBase) && obj.isInTheModel() == false)
				return;
		}
		super.modelChanged(event);
	}
}