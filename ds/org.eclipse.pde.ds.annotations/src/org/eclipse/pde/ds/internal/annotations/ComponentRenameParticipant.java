package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class ComponentRenameParticipant extends RenameParticipant implements ISharableParticipant, ComponentRefactoringParticipant {

	private final ComponentRefactoringHelper helper = new ComponentRefactoringHelper(this);

	@Override
	protected boolean initialize(Object element) {
		return helper.initialize(element);
	}

	@Override
	public String getName() {
		return Messages.ComponentRenameParticipant_name;
	}

	public void addElement(Object element, RefactoringArguments arguments) {
		helper.addElement(element, arguments);
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return helper.checkConditions(pm, context);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return helper.createChange(pm);
	}

	public String getComponentNameRoot(IJavaElement element, RefactoringArguments args) {
		String newName = ((RenameArguments) args).getNewName();
		if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			return newName;
		}

		IType type = (IType) element;
		String compName;
		IType container = type.getDeclaringType();
		if (container == null) {
			compName = String.format("%s.%s", type.getPackageFragment().getElementName(), newName); //$NON-NLS-1$
		} else {
			compName = container.getType(newName).getFullyQualifiedName();
		}

		return compName;
	}
}
