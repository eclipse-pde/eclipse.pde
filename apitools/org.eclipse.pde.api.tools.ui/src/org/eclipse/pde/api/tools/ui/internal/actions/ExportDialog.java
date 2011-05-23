/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.actions;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * A simple input dialog for soliciting an input string from the user.
 * <p>
 * This concrete dialog class can be instantiated as is, or further subclassed as
 * required.
 * </p>
 */
public class ExportDialog extends Dialog {

	//widget state ids 
	static final String SETTINGS_SECTION = ApiUIPlugin.PLUGIN_ID + ".api.exportsession"; //$NON-NLS-1$
	static final String REPORT_PATH_STATE = SETTINGS_SECTION + ".reportpath"; //$NON-NLS-1$
	
	/**
	 * The title of the dialog.
	 */
	private String title;

	/**
	 * The message to display, or <code>null</code> if none.
	 */
	private String message;

	/**
	 * The input value; the empty string by default.
	 */
	private String value = "";//$NON-NLS-1$

	/**
	 * The input validator, or <code>null</code> if none.
	 */
	private IInputValidator validator;

	/**
	 * Ok button widget.
	 */
	private Button okButton;

	/**
	 * Input text widget.
	 */
	private Text text;

	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;

	/**
	 * Error message string.
	 */
	private String errorMessage;
	/**
	 * Constructor
	 * @param provider
	 * @param title
	 */
	public ExportDialog(Shell shell, String title) {
		this(shell,
				title,
				ActionMessages.ExportDialogDescription,
				Util.EMPTY_STRING,
				new IInputValidator() {
					public String isValid(String newText) {
						if (newText != null && newText.length() > 0) {
							if (!newText.endsWith(".html") & !newText.endsWith(".xml")) { //$NON-NLS-1$ //$NON-NLS-2$
								return ActionMessages.ExportDialogErrorMessage;
							}
							return null;
						}
						return ActionMessages.ExportDialogErrorMessage;
					}
				});
	}
	/**
	 * Creates an input dialog with OK and Cancel buttons. Note that the dialog
	 * will have no visual representation (no widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 * 
	 * @param parentShell
	 *            the parent shell, or <code>null</code> to create a top-level
	 *            shell
	 * @param dialogTitle
	 *            the dialog title, or <code>null</code> if none
	 * @param dialogMessage
	 *            the dialog message, or <code>null</code> if none
	 * @param initialValue
	 *            the initial input value, or <code>null</code> if none
	 *            (equivalent to the empty string)
	 * @param validator
	 *            an input validator, or <code>null</code> if none
	 */
	public ExportDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue, IInputValidator validator) {
		super(parentShell);
		this.title = dialogTitle;
		message = dialogMessage;
		if (initialValue == null) {
			value = "";//$NON-NLS-1$
		} else {
			value = initialValue;
		}
		this.validator = validator;
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			value = text.getText();
		} else {
			value = null;
		}
		super.buttonPressed(buttonId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (title != null) {
			shell.setText(title);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		//do this here because setting the text will set enablement on the ok
		// button
		text.setFocus();
		if (value != null) {
			text.setText(value);
			text.selectAll();
		}
		initialize();
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		// create composite
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite comp = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		// create message
		if (message != null) {
			Label label = new Label(comp, SWT.WRAP);
			label.setText(message);
			GridData data = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			data.horizontalSpan = 2;
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			label.setLayoutData(data);
			label.setFont(parent.getFont());
		}
		text = new Text(comp, getInputTextStyle());
		text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		Button browseButton = SWTFactory.createPushButton(comp, ActionMessages.Browse, null, SWT.RIGHT);
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		browseButton.setLayoutData(data);

		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText(ActionMessages.SelectFileName);
				String loctext = ExportDialog.this.getValue().trim();
				if (loctext.length() > 0) {
					File file = new File(loctext).getParentFile();
					if (file != null && file.exists()) {
						dialog.setFilterPath(file.getAbsolutePath());
					}
				}
				String newPath = dialog.open();
				if (newPath != null) {
					ExportDialog.this.getText().setText(newPath);
				}
			}
		});
		errorMessageText = new Text(comp, SWT.READ_ONLY | SWT.WRAP);
		GridData layoutData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL);
		layoutData.horizontalSpan = 2;
		errorMessageText.setLayoutData(layoutData);
		errorMessageText.setBackground(errorMessageText.getDisplay()
				.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		// Set the error message text
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=66292
		setErrorMessage(errorMessage);

		applyDialogFont(comp);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_COMPARE_EXPORT_DIALOG);
		return parent;
	}
	/**
	 * Returns the ok button.
	 * 
	 * @return the ok button
	 */
	protected Button getOkButton() {
		return okButton;
	}

	/**
	 * Returns the text area.
	 * 
	 * @return the text area
	 */
	protected Text getText() {
		return text;
	}

	/**
	 * Returns the validator.
	 * 
	 * @return the validator
	 */
	protected IInputValidator getValidator() {
		return validator;
	}

	/**
	 * Returns the string typed into this input dialog.
	 * 
	 * @return the input string
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Validates the input.
	 * <p>
	 * The default implementation of this framework method delegates the request
	 * to the supplied input validator object; if it finds the input invalid,
	 * the error message is displayed in the dialog's message line. This hook
	 * method is called whenever the text changes in the input field.
	 * </p>
	 */
	protected void validateInput() {
		String errorMessage = null;
		if (validator != null) {
			errorMessage = validator.isValid(text.getText());
		}
		// Bug 16256: important not to treat "" (blank error) the same as null
		// (no error)
		setErrorMessage(errorMessage);
	}

	/**
	 * Sets or clears the error message.
	 * If not <code>null</code>, the OK button is disabled.
	 * 
	 * @param errorMessage
	 *            the error message, or <code>null</code> to clear
	 * @since 3.0
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		if (errorMessageText != null && !errorMessageText.isDisposed()) {
			errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); //$NON-NLS-1$
			// Disable the error message text control if there is no error, or
			// no error text (empty or whitespace only).  Hide it also to avoid
			// color change.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
			boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
			errorMessageText.setEnabled(hasError);
			errorMessageText.setVisible(hasError);
			errorMessageText.getParent().update();
			// Access the ok button by id, in case clients have overridden button creation.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
			Control button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(errorMessage == null);
			}
		}
	}

	/**
	 * Returns the style bits that should be used for the input text field.
	 * Defaults to a single line entry. Subclasses may override.
	 * 
	 * @return the integer style bits that should be used when creating the
	 *         input text
	 * 
	 * @since 3.4
	 */
	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		saveWidgetState();
		super.okPressed();
	}
	/**
	 * Restores the selected item for the given combo based on the stored value from the 
	 * dialog settings
	 * 
	 * @param combo
	 * @param id
	 * @param settings
	 */
	private void restoreTextSelection(String id, IDialogSettings settings) {
		String restoredValue = settings.get(id);
		if(restoredValue != null) {
			this.getText().setText(restoredValue);
		}
	}
	
	/**
	 * Saves the state of the widgets on the page
	 */
	void saveWidgetState() {
		IDialogSettings rootsettings = ApiUIPlugin.getDefault().getDialogSettings();
		IDialogSettings settings = rootsettings.getSection(SETTINGS_SECTION);
		if(settings == null) {
			settings = rootsettings.addNewSection(SETTINGS_SECTION);
		}
		settings.put(REPORT_PATH_STATE, getValue());
	}

	/**
	 * Initializes the controls
	 */
	void initialize() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if(settings != null) {
			restoreTextSelection(REPORT_PATH_STATE, settings);
		}
	}
}
