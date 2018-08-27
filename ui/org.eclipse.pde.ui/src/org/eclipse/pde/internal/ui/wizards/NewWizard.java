/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.Dictionary;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewWizard extends Wizard implements INewWizard, IDefaultValueConsumer {
	private org.eclipse.ui.IWorkbench workbench;
	private org.eclipse.jface.viewers.IStructuredSelection selection;
	private Dictionary<?, ?> defaultValues;

	public NewWizard() {
		super();
		setWindowTitle(PDEUIMessages.NewWizard_wtitle);
	}

	public org.eclipse.jface.viewers.IStructuredSelection getSelection() {
		return selection;
	}

	public IWorkbench getWorkbench() {
		return workbench;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public final String getDefaultValue(String key) {
		if (defaultValues == null)
			return null;
		return (String) defaultValues.get(key);
	}

	@Override
	public final void init(Dictionary<?, ?> defaultValues) {
		this.defaultValues = defaultValues;
	}
}
