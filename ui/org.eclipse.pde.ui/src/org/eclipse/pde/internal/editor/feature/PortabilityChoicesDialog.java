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
import org.eclipse.pde.internal.elements.DefaultContentProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import java.util.StringTokenizer;
import org.eclipse.pde.internal.wizards.ListUtil;

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

	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
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

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_CHOICES));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		choiceViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		choiceViewer.setContentProvider(new ContentProvider());
		choiceViewer.setLabelProvider(new ChoiceLabelProvider());
		choiceViewer.setSorter(ListUtil.NAME_SORTER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 350;
		choiceViewer.getTable().setLayoutData(gd);

		Composite buttonContainer = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		buttonContainer.setLayout(layout);

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});
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
			choiceViewer.setCheckedElements(selected.toArray());
		}
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

	private void handleSelectAll(boolean doSelect) {
		choiceViewer.setAllChecked(doSelect);
	}

	private String computeNewValue() {
		Object[] checked = choiceViewer.getCheckedElements();
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