package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RefactoringActionFactory {
	
	public static PDERefactoringAction createRefactorPluginIdAction() {
		return createRefactorPluginIdAction(PDEUIMessages.RenamePluginAction_label);
	}
	
	public static PDERefactoringAction createRefactorPluginIdAction(String label) {
		return new PDERefactoringAction(label, new RefactoringPluginInfo()) {

			public RefactoringProcessor getRefactoringProcessor(
					RefactoringInfo info) {
				return new RenamePluginProcessor(info);
			}

			public RefactoringWizard getRefactoringWizard(PDERefactor refactor,
					RefactoringInfo info) {
				return new RenamePluginWizard(refactor, info);
			}
			
		};
	}
	
	public static PDERefactoringAction createRefactorExtPointAction(String label) {
		return new PDERefactoringAction(label, getExtensionPointInfo()) {

			public RefactoringProcessor getRefactoringProcessor(
					RefactoringInfo info) {
				return new RenameExtensionPointProcessor(info);
			}

			public RefactoringWizard getRefactoringWizard(PDERefactor refactor,
					RefactoringInfo info) {
				return new RenameExtensionPointWizard(refactor, info);
			}
			
		};
	}
	
	private static RefactoringInfo getExtensionPointInfo() {
		return new RefactoringInfo() {

			public IPluginModelBase getBase() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint)fSelection).getPluginModel();
				}
				return null;
			}

			public String getCurrentValue() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint)fSelection).getId();
				}
				return null;
			}
			
		};
	}

}
