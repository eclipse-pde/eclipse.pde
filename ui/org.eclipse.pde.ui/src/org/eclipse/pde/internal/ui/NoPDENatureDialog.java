/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui;

import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.SWT;

/**
 * @version 	1.0
 * @author
 */
public class NoPDENatureDialog extends MessageDialog {

	public static final int STOP_WARNING = 0;
	public static final int KEEP_WARNING = 1;
	public static final int OPEN_WIZARD = 2;
	
	private static final String KEY_STOP_WARNING = "MissingPDENature.stopWarning";
	private static final String KEY_KEEP_WARNING = "MissingPDENature.keepWarning";
	private static final String KEY_OPEN_WIZARD = "MissingPDENature.openWizard";

	private Button stopWarningButton;
	private Button keepWarningButton;
	private Button openWizardButton;
	private int result;

	public NoPDENatureDialog(Shell parent, String title, String message) {
		super(parent, title, null, // accept the default window icon
		message, INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
	}

	public int getResult() {
		return result;
	}

	protected Control createCustomArea(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		buttons.setLayout(layout);

		stopWarningButton = createButton(buttons, PDEPlugin.getResourceString(KEY_STOP_WARNING));
		keepWarningButton = createButton(buttons, PDEPlugin.getResourceString(KEY_KEEP_WARNING));
		openWizardButton = createButton(buttons, PDEPlugin.getResourceString(KEY_OPEN_WIZARD));
		keepWarningButton.setSelection(true);
		return buttons;
	}

	private Button createButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		button.setLayoutData(gd);
		return button;
	}

	protected void buttonPressed(int buttonId) {
		storeResult();
		super.buttonPressed(buttonId);
	}

	private void storeResult() {
		if (stopWarningButton.getSelection()) {
			result = STOP_WARNING;
		} else if (keepWarningButton.getSelection()) {
			result = KEEP_WARNING;
		} else
			result = OPEN_WIZARD;
	}
}