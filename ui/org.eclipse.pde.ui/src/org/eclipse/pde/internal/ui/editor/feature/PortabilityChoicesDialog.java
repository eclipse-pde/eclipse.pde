/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class PortabilityChoicesDialog extends TrayDialog {
	private String value;
	private Choice[] choices;
	private CheckboxTableViewer choiceViewer;
	private WizardCheckboxTablePart checkboxTablePart;

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return choices;
		}
	}

	static class ChoiceLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return ((Choice) obj).getLabel();
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public PortabilityChoicesDialog(Shell shell, Choice[] choices, String value) {
		super(shell);
		this.value = value;
		this.choices = choices;

		checkboxTablePart = new WizardCheckboxTablePart(PDEUIMessages.FeatureEditor_PortabilityChoicesDialog_choices);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
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
		choiceViewer.setLabelProvider(new ChoiceLabelProvider());

		gd = (GridData) checkboxTablePart.getControl().getLayoutData();
		gd.widthHint = 300;
		gd.heightHint = 350;

		Dialog.applyDialogFont(parent);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.FEATURE_PORTABILITY_WIZARD);
		return container;
	}

	public String getValue() {
		return value;
	}

	protected void initialize() {
		choiceViewer.setInput(PDEPlugin.getDefault());

		if (value != null) {
			ArrayList<Choice> selected = new ArrayList<>();
			StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
			while (stok.hasMoreElements()) {
				String tok = stok.nextToken();
				Choice choice = findChoice(tok);
				if (choice != null)
					selected.add(choice);
			}
			checkboxTablePart.setSelection(selected.toArray());
		} else
			checkboxTablePart.selectAll(false);
	}

	private Choice findChoice(String value) {
		for (Choice choice : choices) {
			if (choice.getValue().equalsIgnoreCase(value))
				return choice;
		}
		return null;
	}

	@Override
	protected void okPressed() {
		value = computeNewValue();
		super.okPressed();
	}

	private String computeNewValue() {
		Object[] checked = checkboxTablePart.getSelection();
		if (checked.length == 0)
			return ""; //$NON-NLS-1$
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < checked.length; i++) {
			Choice choice = (Choice) checked[i];
			if (i > 0)
				buf.append(","); //$NON-NLS-1$
			buf.append(choice.getValue());
		}
		return buf.toString();
	}
}
