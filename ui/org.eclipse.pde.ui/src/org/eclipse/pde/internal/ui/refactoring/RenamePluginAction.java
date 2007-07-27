/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class RenamePluginAction extends Action {
	
	private RenamePluginInfo fInfo = new RenamePluginInfo();
	
	public RenamePluginAction() {
		super(PDEUIMessages.RenamePluginAction_label);
	}
	
	public void setPlugin(IPluginModelBase base) {
		fInfo.setBase(base);
	}

	public void run() {
		RenamePluginProcessor processor = new RenamePluginProcessor(fInfo);
		PDERefactor refactor = new PDERefactor(processor);
		RenamePluginWizard wizard = new RenamePluginWizard(refactor, fInfo);
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation( wizard );
		
	    try {
	      op.run( getShell(), "" ); //$NON-NLS-1$
	    } catch( final InterruptedException irex ) {
	    }
	}
	
	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

}
