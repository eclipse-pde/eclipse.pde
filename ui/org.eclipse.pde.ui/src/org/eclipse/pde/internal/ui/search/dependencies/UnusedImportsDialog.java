/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil.PluginComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class UnusedImportsDialog extends TrayDialog {
	private IPluginModelBase model;
	private Object[] unused;
	private WizardCheckboxTablePart checkboxTablePart;
	private CheckboxTableViewer choiceViewer;

	static class Comparator extends PluginComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1.getClass() == e2.getClass())
				return super.compare(viewer, e1, e2);
			else if (e1 instanceof ImportPackageObject)
				return 1;
			else
				return -1;
		}
	}

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return unused;
		}
	}

	public UnusedImportsDialog(Shell parentShell, IPluginModelBase model, Object[] unused) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.model = model;
		this.unused = unused;
		checkboxTablePart = new WizardCheckboxTablePart(PDEUIMessages.UnusedDependencies_remove);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.UNUSED_IMPORTS_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 9;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		checkboxTablePart.createControl(container);
		choiceViewer = checkboxTablePart.getTableViewer();
		choiceViewer.setContentProvider(new ContentProvider());
		choiceViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		choiceViewer.setComparator(new Comparator());

		gd = (GridData) checkboxTablePart.getControl().getLayoutData();
		gd.widthHint = 250;
		gd.heightHint = 275;

		choiceViewer.setInput(PDEPlugin.getDefault());
		checkboxTablePart.setSelection(unused);
		return container;
	}

	@Override
	protected void okPressed() {
		GatherUnusedDependenciesOperation.removeDependencies(model, choiceViewer.getCheckedElements());
		super.okPressed();
	}

}
