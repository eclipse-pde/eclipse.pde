/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.ui.internal.actions.ActionMessages;
import org.eclipse.pde.api.tools.ui.internal.wizards.CompareToBaselineWizard;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Default handler for the compare to baseline command
 * 
 * @since 1.0.500
 */
public class CompareToBaselineHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			CompareToBaselineWizard wizard = new CompareToBaselineWizard(structuredSelection, ActionMessages.CompareDialogTitle);
			WizardDialog wdialog = new WizardDialog(HandlerUtil.getActiveShell(event), wizard);
			wdialog.open();
		}
		return null;
	}

}
