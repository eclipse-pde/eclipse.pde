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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.PluginElementNode;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLInsertionComputer;

public class NewElementAction extends Action {
	public static final String UNKNOWN_ELEMENT_TAG = PDEUIMessages.NewElementAction_generic;

	private ISchemaElement elementInfo;

	private IPluginParent parent;

	public NewElementAction(ISchemaElement elementInfo, IPluginParent parent) {
		this.elementInfo = elementInfo;
		// this.project = project;
		this.parent = parent;
		setText(getElementName());
		setImageDescriptor(PDEPluginImages.DESC_GENERIC_XML_OBJ);
		setEnabled(parent.getModel().isEditable());
	}

	private String getElementName() {
		return elementInfo != null ? elementInfo.getName() : UNKNOWN_ELEMENT_TAG;
	}

	public void run() {
		IPluginElement newElement = parent.getModel().getFactory().createElement(parent);
		try {
			newElement.setName(getElementName());
			((PluginElementNode) newElement).setParentNode((IDocumentElementNode) parent);

			// If there is an associated schema, recursively auto-insert 
			// required child elements and attributes respecting multiplicity
			if (elementInfo != null) {
				XMLInsertionComputer.computeInsertion(elementInfo, newElement);
			}
			parent.add(newElement);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

}
