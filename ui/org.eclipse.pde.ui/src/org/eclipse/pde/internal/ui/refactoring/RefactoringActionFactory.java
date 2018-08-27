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

			@Override
			public RefactoringProcessor getRefactoringProcessor(RefactoringInfo info) {
				return new RenamePluginProcessor(info);
			}

			@Override
			public RefactoringWizard getRefactoringWizard(PDERefactor refactor, RefactoringInfo info) {
				return new RenamePluginWizard(refactor, info);
			}

		};
	}

	public static PDERefactoringAction createRefactorExtPointAction(String label) {
		return new PDERefactoringAction(label, getExtensionPointInfo()) {

			@Override
			public RefactoringProcessor getRefactoringProcessor(RefactoringInfo info) {
				return new RenameExtensionPointProcessor(info);
			}

			@Override
			public RefactoringWizard getRefactoringWizard(PDERefactor refactor, RefactoringInfo info) {
				return new RenameExtensionPointWizard(refactor, info);
			}

		};
	}

	private static RefactoringInfo getExtensionPointInfo() {
		return new RefactoringInfo() {

			@Override
			public IPluginModelBase getBase() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint) fSelection).getPluginModel();
				}
				return null;
			}

			@Override
			public String getCurrentValue() {
				if (fSelection instanceof IPluginExtensionPoint) {
					return ((IPluginExtensionPoint) fSelection).getId();
				}
				return null;
			}

		};
	}

}
