/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;

public abstract class AbstractPluginElementDetails extends PDEDetails {

	private final PDESection fMasterSection;

	public AbstractPluginElementDetails(PDESection masterSection) {
		fMasterSection = masterSection;
	}

	public PDESection getMasterSection() {
		return fMasterSection;
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		// TODO reveal the keybinding Ctrl+F to the user, ideally by showing the action in the context menu
		if (actionId.equals(ActionFactory.FIND.getId())) {
			if (fMasterSection != null && fMasterSection instanceof ExtensionsSection) {
				final Control focusControl = Display.getCurrent().getFocusControl(); // getPage().getLastFocusControl();
				String filterText = (focusControl instanceof Text) ? ((Text) focusControl).getText() : (focusControl instanceof CCombo) ? ((CCombo) focusControl).getText() : null;
				if (filterText != null) {
					// add value of the currently focused attribute text to the filter
					((ExtensionsSection) fMasterSection).addAttributeToFilter(filterText, true);
					Display.getCurrent().asyncExec(() -> getPage().updateFormSelection());
				}
			}
		}
		return super.doGlobalAction(actionId);
	}

}