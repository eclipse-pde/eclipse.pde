/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;

public class SimpleCSRemoveRunObjectAction extends Action {

	private ISimpleCSRunContainerObject fRunContainerObject;

	public SimpleCSRemoveRunObjectAction() {
		setText(SimpleActionMessages.SimpleCSRemoveRunObjectAction_actionText);
//		setImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}

	public void setRunObject(ISimpleCSRunContainerObject runObject) {
		fRunContainerObject = runObject;
	}

	@Override
	public void run() {
		if (fRunContainerObject != null) {
			// Determine parent type and remove accordingly
			ISimpleCSObject parent = fRunContainerObject.getParent();
			if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem item = (ISimpleCSItem) parent;
				item.setExecutable(null);
			} else if (parent.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem) parent;
				subitem.setExecutable(null);
			} else if (parent.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) {
				// Specifically for perform-when edge case
				// Action and command supported; but, will never be applicable
				if ((fRunContainerObject.getType() == ISimpleCSConstants.TYPE_ACTION) || (fRunContainerObject.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
					ISimpleCSPerformWhen performWhen = (ISimpleCSPerformWhen) parent;
					performWhen.removeExecutable((ISimpleCSRunObject) fRunContainerObject);
				}
			}
		}
	}
}
