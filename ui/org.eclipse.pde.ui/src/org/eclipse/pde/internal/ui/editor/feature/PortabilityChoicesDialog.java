/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class PortabilityChoicesDialog extends Dialog {
	private static final String KEY_CHOICES =
		"FeatureEditor.PortabilityChoicesDialog.choices"; //$NON-NLS-1$
	private String value;
	private Choice[] choices;
	private CheckboxTableViewer choiceViewer;
	private WizardCheckboxTablePart checkboxTablePart;

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return choices;
		}
	}

	class ChoiceLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return ((Choice) obj).getLabel();
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public PortabilityChoicesDialog(
		Shell shell,
		Choice[] choices,
		String value) {
		super(shell);
		this.value = value;
		this.choices = choices;
		
		checkboxTablePart = new WizardCheckboxTablePart(PDEPlugin.getResourceString(KEY_CHOICES));
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
	}

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

		gd = (GridData)checkboxTablePart.getControl().getLayoutData();
		gd.widthHint = 300;
		gd.heightHint = 350;

		initialize();
		WorkbenchHelp.setHelp(container, IHelpContextIds.FEATURE_PORTABILITY_WIZARD);
		return container;
	}
	public String getValue() {
		return value;
	}

	protected void initialize() {
		choiceViewer.setInput(PDEPlugin.getDefault());

		if (value != null) {
			Vector selected = new Vector();
			StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
			while (stok.hasMoreElements()) {
				String tok = stok.nextToken();
				Choice choice = findChoice(tok);
				if (choice != null)
					selected.add(choice);
			}
			checkboxTablePart.setSelection(selected.toArray());
		}
		else 
			checkboxTablePart.selectAll(false);
	}

	private Choice findChoice(String value) {
		for (int i = 0; i < choices.length; i++) {
			Choice choice = choices[i];
			if (choice.getValue().equalsIgnoreCase(value))
				return choice;
		}
		return null;
	}

	protected void okPressed() {
		value = computeNewValue();
		super.okPressed();
	}

	private String computeNewValue() {
		Object[] checked = checkboxTablePart.getSelection();
		if (checked.length == 0)
			return ""; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < checked.length; i++) {
			Choice choice = (Choice) checked[i];
			if (i > 0)
				buf.append(","); //$NON-NLS-1$
			buf.append(choice.getValue());
		}
		return buf.toString();
	}
}
