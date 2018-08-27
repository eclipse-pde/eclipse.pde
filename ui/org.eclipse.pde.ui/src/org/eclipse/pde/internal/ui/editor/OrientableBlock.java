/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public abstract class OrientableBlock extends PDEMasterDetailsBlock {

	public OrientableBlock(PDEFormPage page) {
		super(page);
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();

		Action haction = new Action("hor", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText(PDEUIMessages.DetailsBlock_horizontal);
		haction.setImageDescriptor(PDEPluginImages.DESC_HORIZONTAL);
		haction.setDisabledImageDescriptor(PDEPluginImages.DESC_HORIZONTAL_DISABLED);

		Action vaction = new Action("ver", IAction.AS_RADIO_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText(PDEUIMessages.DetailsBlock_vertical);
		vaction.setImageDescriptor(PDEPluginImages.DESC_VERTICAL);
		vaction.setDisabledImageDescriptor(PDEPluginImages.DESC_VERTICAL_DISABLED);
		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}
}