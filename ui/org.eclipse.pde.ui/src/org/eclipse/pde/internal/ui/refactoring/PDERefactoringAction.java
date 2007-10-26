package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public abstract class PDERefactoringAction extends Action {
	
	private RefactoringInfo fInfo = null;
	
	public PDERefactoringAction(String label, RefactoringInfo info) {
		super(label);
		fInfo = info;
	}
	
	public void setSelection(Object selection) {
		fInfo.setSelection(selection);
	}

	public void run() {
		RefactoringProcessor processor = getRefactoringProcessor(fInfo);
		PDERefactor refactor = new PDERefactor(processor);
		RefactoringWizard wizard = getRefactoringWizard(refactor, fInfo);
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation( wizard );
		
	    try {
	      op.run( getShell(), "" ); //$NON-NLS-1$
	    } catch( final InterruptedException irex ) {
	    }
	}
	
	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}
	
	public abstract RefactoringProcessor getRefactoringProcessor(RefactoringInfo info);
	
	public abstract RefactoringWizard getRefactoringWizard(PDERefactor refactor, RefactoringInfo info);
}
