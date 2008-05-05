/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> 
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.ui.Messages;

public class DSAddItemAction extends Action {

	private IDSObject fSelection;

	private IDSObject fParent;

	private int fType;

	private IDSObject fNewObject;

	/**
	 * 
	 */
	public DSAddItemAction() {
		setText(Messages.DSAddItemAction_0);
		fType = -1;
		fSelection = null;
		fNewObject = null;
	}

	public void setSelection(IDSObject selection) {
		this.fSelection = selection;
	}

	public void setType(int type) {
		this.fType = type;
	}

	public void run() {
		if (fSelection == null || fType == -1) {
			return;
		} else {
			// Create the new item
			IDSObject newItem = createNewItem();
			// Insert the new item
			fParent.addChildNode(newItem);
		}
	}

	private IDSObject createNewItem() {
		IDSDocumentFactory factory = fSelection.getModel().getFactory();

		fParent = fSelection.getModel().getDSRoot();

		switch (fType) {
		case IDSConstants.TYPE_SERVICE:
			fNewObject = factory.createService();
			break;
		case IDSConstants.TYPE_PROPERTIES:
			fNewObject = factory.createProperties();
			break;
		case IDSConstants.TYPE_PROPERTY:
			fNewObject = factory.createProperty();
			break;
		case IDSConstants.TYPE_PROVIDE:
			fNewObject = factory.createProvide();
			// only provide component isn't a child of DSRoot component
			fParent = fSelection;
			break;
		case IDSConstants.TYPE_REFERENCE:
			fNewObject = factory.createReference();
			break;
		}
		return fNewObject;

	}

	public IDSObject getFNewObject() {
		return fNewObject;
	}
}
