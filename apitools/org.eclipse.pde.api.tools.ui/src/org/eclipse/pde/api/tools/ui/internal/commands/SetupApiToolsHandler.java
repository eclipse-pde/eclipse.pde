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
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.actions.ActionMessages;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiToolingSetupRefactoring;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiToolingSetupWizard;

/**
 * Default handler for the setup API Tools command
 *
 * @since 1.0.500
 */
public class SetupApiToolsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ApiToolingSetupWizard wizard = new ApiToolingSetupWizard(new ApiToolingSetupRefactoring());
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(ApiUIPlugin.getShell(), ActionMessages.ApiToolingSetupObjectContribution_0);
		} catch (InterruptedException ie) {
			ApiUIPlugin.log(ie);
		}
		return null;
	}
}
