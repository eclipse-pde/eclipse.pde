/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public abstract class PDEMasterDetailsBlock extends MasterDetailsBlock {
	private PDEFormPage page;
	
	public PDEMasterDetailsBlock(PDEFormPage page) {
		this.page = page;
	}
	
	public PDEFormPage getPage() {
		return page;
	}
	
	protected abstract PDESection createMasterSection(IManagedForm managedForm, Composite parent);

	protected void createMasterPart(final IManagedForm managedForm,
			Composite parent) {
		PDESection section = createMasterSection(managedForm, parent);
		managedForm.addPart(section);
		Section sc = section.getSection();
		sc.marginWidth = 5;
		sc.marginHeight = 5;
	}
	protected void createToolBarActions(IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
	
		Action haction = new Action("hor", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.HORIZONTAL);
				form.reflow(true);
			}
		};
		haction.setChecked(true);
		haction.setToolTipText(PDEPlugin.getResourceString("DetailsBlock.horizontal")); //$NON-NLS-1$
		haction.setImageDescriptor(PDEPluginImages.DESC_HORIZONTAL);
		haction.setDisabledImageDescriptor(PDEPluginImages.DESC_HORIZONTAL_DISABLED);

		Action vaction = new Action("ver", Action.AS_RADIO_BUTTON) { //$NON-NLS-1$
			public void run() {
				sashForm.setOrientation(SWT.VERTICAL);
				form.reflow(true);
			}
		};
		vaction.setChecked(false);
		vaction.setToolTipText(PDEPlugin.getResourceString("DetailsBlock.vertical")); //$NON-NLS-1$
		vaction.setImageDescriptor(PDEPluginImages.DESC_VERTICAL);
		vaction.setDisabledImageDescriptor(PDEPluginImages.DESC_VERTICAL_DISABLED);
		form.getToolBarManager().add(haction);
		form.getToolBarManager().add(vaction);
	}
}
