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
import org.eclipse.pde.api.tools.ui.internal.wizards.JavadocConversionRefactoring;
import org.eclipse.pde.api.tools.ui.internal.wizards.JavadocConversionWizard;

/**
 * Default handler for the convert Javadoc command
 * 
 * @since 1.0.500
 */
public class ConvertJavadocTagsHandler extends AbstractHandler {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		JavadocConversionWizard wizard = new JavadocConversionWizard(new JavadocConversionRefactoring());
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(ApiUIPlugin.getShell(), Messages.ConvertJavadocTagsHandler_0);
		} catch (InterruptedException ie) {
			ApiUIPlugin.log(ie);
		}
		return null;
	}
}
