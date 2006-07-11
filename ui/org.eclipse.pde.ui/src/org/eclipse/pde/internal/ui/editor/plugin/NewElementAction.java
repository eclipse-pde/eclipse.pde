/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

public class NewElementAction extends Action {
	public static final String UNKNOWN_ELEMENT_TAG = PDEUIMessages.NewElementAction_generic; 

	private ISchemaElement elementInfo;

	private IPluginParent parent;

	private IProject project;

	public NewElementAction(ISchemaElement elementInfo, IPluginParent parent) {
		this.elementInfo = elementInfo;
		// this.project = project;
		this.parent = parent;
		setText(getElementName());
		setImageDescriptor(PDEPluginImages.DESC_GENERIC_XML_OBJ);
		IResource resource = parent.getModel().getUnderlyingResource();
		if (resource != null)
			project = resource.getProject();
		setEnabled(parent.getModel().isEditable());
	}

	private String getElementName() {
		return elementInfo != null ? elementInfo.getName() : UNKNOWN_ELEMENT_TAG;
	}

	private void initializeAttribute(IPluginElement element, ISchemaAttribute attInfo,
			int counter) throws CoreException {
		String value = null;
		if (attInfo.getKind() == IMetaAttribute.JAVA)
			value = XMLUtil.createDefaultClassName(project, attInfo, counter);
		else if (attInfo.getUse() == ISchemaAttribute.DEFAULT
				&& attInfo.getValue() != null)
			value = attInfo.getValue().toString();
		else if (attInfo.getType().getRestriction() != null)
			value = attInfo.getType().getRestriction().getChildren()[0].toString();
		else
			value = XMLUtil.createDefaultName(project, attInfo, counter);

		element.setAttribute(attInfo.getName(), value);
	}

	private void initializeAttributes(IPluginElement element) throws CoreException {
		ISchemaElement elementInfo = (ISchemaElement) element.getElementInfo();
		if (elementInfo == null)
			return;
		int counter = XMLUtil.getCounterValue(elementInfo);
		ISchemaAttribute[] attributes = elementInfo.getAttributes();
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute attInfo = attributes[i];
			if (attInfo.getUse() == ISchemaAttribute.REQUIRED
					|| attInfo.getUse() == ISchemaAttribute.DEFAULT)
				initializeAttribute(element, attInfo, counter);
		}
	}

	public void run() {
		IPluginElement newElement = parent.getModel().getFactory().createElement(parent);
		try {
			newElement.setName(getElementName());
			initializeAttributes(newElement);
			parent.add(newElement);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}


}
