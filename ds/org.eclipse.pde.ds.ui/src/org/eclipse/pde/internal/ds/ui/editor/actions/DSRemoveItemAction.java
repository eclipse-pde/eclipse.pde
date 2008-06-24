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
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> 
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.ui.Messages;

public class DSRemoveItemAction extends Action {
	private IDSObject fItem;
	private IDSObject fObjectToSelect;

	/**
	 * 
	 */
	public DSRemoveItemAction() {
		setText(Messages.DSRemoveItemAction_actionText);
		fItem = null;
	}

	/**
	 * @param item
	 */
	public void setItem(IDSObject item) {
		fItem = item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fItem != null) {
			// Determine parent type and remove accordingly
			IDSObject parent = (IDSObject) fItem.getParentNode();
			if (parent.getType() == IDSConstants.TYPE_COMPONENT) {
				// Parent is a component
				IDSComponent item = (IDSComponent) parent;
				fObjectToSelect = parent;
				// Remove the subitem
				item.removeChildNode(fItem, true);
			} else if (parent.getType() == IDSConstants.TYPE_SERVICE) {
				// Parent is a service
				IDSService item = (IDSService) parent;
				fObjectToSelect = parent;
				// Remove the subitem
				item.removeChildNode(fItem, true);
			}
		}
	}

	/**
	 * @return
	 */
	public IDSObject getObjectToSelect() {
		return fObjectToSelect;
	}
}
