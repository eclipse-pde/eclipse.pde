package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.Vector;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.events.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.*;
import java.util.Hashtable;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.pde.internal.elements.DefaultTableProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import java.util.StringTokenizer;
import org.eclipse.pde.internal.wizards.ListUtil;
import org.eclipse.pde.internal.parts.WizardCheckboxTablePart;

public class PortabilityChoicesDialog extends Dialog {
	private static final String KEY_CHOICES =
		"FeatureEditor.PortabilityChoicesDialog.choices";
	private static final String KEY_SELECT_ALL =
		"FeatureEditor.PortabilityChoicesDialog.selectAll";
	private static final String KEY_DESELECT_ALL =
		"FeatureEditor.PortabilityChoicesDialog.deselectAll";
	private Button okButton;
	private String value;
	private PortabilityChoice[] choices;
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
			return ((PortabilityChoice) obj).getLabel();
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public PortabilityChoicesDialog(
		Shell shell,
		PortabilityChoice[] choices,
		String value) {
		super(shell);
		this.value = value;
		this.choices = choices;
		
		checkboxTablePart = new WizardCheckboxTablePart(PDEPlugin.getResourceString(KEY_CHOICES));
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton =
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
		return container;
	}
	public String getValue() {
		return value;
	}

	protected void initialize() {
		choiceViewer.setInput(PDEPlugin.getDefault());

		if (value != null) {
			Vector selected = new Vector();
			StringTokenizer stok = new StringTokenizer(value, ",");
			while (stok.hasMoreElements()) {
				String tok = stok.nextToken();
				PortabilityChoice choice = findChoice(tok);
				if (choice != null)
					selected.add(choice);
			}
			checkboxTablePart.setSelection(selected.toArray());
		}
		else 
			checkboxTablePart.selectAll(false);
	}

	private PortabilityChoice findChoice(String value) {
		for (int i = 0; i < choices.length; i++) {
			PortabilityChoice choice = choices[i];
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
			return "";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < checked.length; i++) {
			PortabilityChoice choice = (PortabilityChoice) checked[i];
			if (i > 0)
				buf.append(",");
			buf.append(choice.getValue());
		}
		return buf.toString();
	}
}