/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.stubs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.pde.api.tools.internal.provisional.stubs.Converter;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

public class ConverterUI extends Dialog {

	static final int ALL_ID = IDialogConstants.CLIENT_ID + 17;
	static final int ARCHIVE_FILES_ID = IDialogConstants.CLIENT_ID + 13;
	static final int CLASS_FILES_ID = IDialogConstants.CLIENT_ID + 12;
	static final String[] COMMAND_LINE_OPTIONS;
	static final int COMPRESS_ID = IDialogConstants.CLIENT_ID + 14;
	static final int INPUT_ID = IDialogConstants.CLIENT_ID + 10;
	static final int KEEP_ALL_ID = IDialogConstants.CLIENT_ID + 7;
	static final int KEEP_ID = IDialogConstants.CLIENT_ID + 1;
	static final int KEEP_NONE_ID = IDialogConstants.CLIENT_ID + 8;
	static final int KEEP_PACKAGE_ID = IDialogConstants.CLIENT_ID + 5;
	static final int KEEP_PRIVATE_ID = IDialogConstants.CLIENT_ID + 2;
	static final int KEEP_PROTECTED_ID = IDialogConstants.CLIENT_ID + 3;
	static final int KEEP_PUBLIC_ID = IDialogConstants.CLIENT_ID + 4;
	static final int KEEP_SYNTHETIC_ID = IDialogConstants.CLIENT_ID + 6;
	static final int OUTPUT_ID = IDialogConstants.CLIENT_ID + 9;
	static final int RECURSE_ID = IDialogConstants.CLIENT_ID + 15;
	static final int REFS_ID = IDialogConstants.CLIENT_ID + 16;
	static final int SKIP_RESOURCE_FILES_ID = IDialogConstants.CLIENT_ID + 18;

	static final int VERBOSE_ID = IDialogConstants.CLIENT_ID + 11;
	static {
		COMMAND_LINE_OPTIONS = new String[SKIP_RESOURCE_FILES_ID - IDialogConstants.CLIENT_ID];
		COMMAND_LINE_OPTIONS[KEEP_ID - IDialogConstants.CLIENT_ID - 1] = "keep"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_PRIVATE_ID - IDialogConstants.CLIENT_ID - 1] = "private"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_PROTECTED_ID - IDialogConstants.CLIENT_ID - 1] = "protected"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_PUBLIC_ID - IDialogConstants.CLIENT_ID - 1] = "public"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_PACKAGE_ID - IDialogConstants.CLIENT_ID - 1] = "package"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_SYNTHETIC_ID - IDialogConstants.CLIENT_ID - 1] = "synthetic"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_ALL_ID - IDialogConstants.CLIENT_ID - 1] = "all"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[KEEP_NONE_ID - IDialogConstants.CLIENT_ID - 1] = "none"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[OUTPUT_ID - IDialogConstants.CLIENT_ID - 1] = "-output"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[INPUT_ID - IDialogConstants.CLIENT_ID - 1] = "-input"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[VERBOSE_ID - IDialogConstants.CLIENT_ID - 1] = "-v"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[CLASS_FILES_ID - IDialogConstants.CLIENT_ID - 1] = "-classfiles"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[COMPRESS_ID - IDialogConstants.CLIENT_ID - 1] = "-compress"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[ARCHIVE_FILES_ID - IDialogConstants.CLIENT_ID - 1] = "-archives"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[REFS_ID - IDialogConstants.CLIENT_ID - 1] = "-refs"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[RECURSE_ID - IDialogConstants.CLIENT_ID - 1] = "-s"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[ALL_ID - IDialogConstants.CLIENT_ID - 1] = "-all"; //$NON-NLS-1$
		COMMAND_LINE_OPTIONS[SKIP_RESOURCE_FILES_ID - IDialogConstants.CLIENT_ID - 1] = "-skipresourcefiles"; //$NON-NLS-1$
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		ConverterUI instance = new ConverterUI(shell);
		instance.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	List commandLine;
	String input;
	Text inputText;

	boolean isFileInput;
	Map options;
	
	String output;

	Text outputText;

	String title;

	List widgets;

	public ConverterUI(Shell parent) {
		super(parent);
		this.title = Messages.ConverterUI_18;
		this.widgets = new ArrayList();
		this.commandLine = new ArrayList();
		this.options = new HashMap();
		createDialogArea(parent);
	}

	void addNewOption(Composite composite, int id, String optionLabel, boolean selection) {
		Button button = new Button(composite, SWT.CHECK);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		button.setText(optionLabel);
		button.setSelection(selection);
		Integer idObject = new Integer(id);
		button.setData(idObject);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				buttonPressed(((Integer) event.widget.getData()).intValue());
			}
		});
		this.options.put(idObject, button);
		this.widgets.add(button);
	}

	private void addToCommandLine(String argument) {
		this.commandLine.add(argument);
	}

	protected void buttonPressed(int buttonId) {
		switch(buttonId) {
			case KEEP_PRIVATE_ID :
			case KEEP_PROTECTED_ID :
			case KEEP_PUBLIC_ID :
			case KEEP_PACKAGE_ID :
			case KEEP_SYNTHETIC_ID :
			case KEEP_ALL_ID :
			case KEEP_NONE_ID :
				
				break;
			case OUTPUT_ID :
				DirectoryDialog directoryDialog = new DirectoryDialog(new Shell(), SWT.PRIMARY_MODAL);
				String result = directoryDialog.open();
				this.outputText.setText(result == null ? "" : result); //$NON-NLS-1$
				this.output = result;
				this.getShell().setFocus();
				break;
			case INPUT_ID :
				result = null;
				if (this.isFileInput) {
					FileDialog fileDialog = new FileDialog(new Shell(), SWT.PRIMARY_MODAL);
					result = fileDialog.open();
				} else {
					directoryDialog = new DirectoryDialog(new Shell(), SWT.PRIMARY_MODAL);
					result = directoryDialog.open();
				}
				this.inputText.setText(result == null ? "" : result); //$NON-NLS-1$
				this.input = result;
				this.getShell().setFocus();
				break;
			case VERBOSE_ID :
			case COMPRESS_ID :
			case RECURSE_ID :
			case REFS_ID :
			case SKIP_RESOURCE_FILES_ID :
			case ALL_ID :
				String commandLineOption = getCommandLineOptions(buttonId);
				Button button = (Button) this.options.get(new Integer(buttonId));
				if (button.getSelection()) {
					if (!this.commandLine.contains(commandLineOption)) {
						this.addToCommandLine(commandLineOption);
					}
				} else if (this.commandLine.contains(commandLineOption)) {
					this.removeFromCommandLine(commandLineOption);
				}
				break;
			case CLASS_FILES_ID :
				commandLineOption = getCommandLineOptions(buttonId);
				button = (Button) this.options.get(new Integer(buttonId));
				if (button.getSelection()) {
					// check if archives option is set
					Button button2 = (Button) this.options.get(new Integer(ARCHIVE_FILES_ID));
					if (button2.getSelection()) {
						this.removeFromCommandLine(getCommandLineOptions(ARCHIVE_FILES_ID));
						this.removeFromCommandLine(getCommandLineOptions(CLASS_FILES_ID));
						this.addToCommandLine(getCommandLineOptions(ALL_ID));
					} else {
						this.addToCommandLine(getCommandLineOptions(CLASS_FILES_ID));
					}
				} else {
					this.removeFromCommandLine(getCommandLineOptions(CLASS_FILES_ID));
				}
				break;
			case ARCHIVE_FILES_ID :
				commandLineOption = getCommandLineOptions(buttonId);
				button = (Button) this.options.get(new Integer(buttonId));
				if (button.getSelection()) {
					// check if archives option is set
					Button button2 = (Button) this.options.get(new Integer(CLASS_FILES_ID));
					if (button2.getSelection()) {
						this.removeFromCommandLine(getCommandLineOptions(ARCHIVE_FILES_ID));
						this.removeFromCommandLine(getCommandLineOptions(CLASS_FILES_ID));
						this.addToCommandLine(getCommandLineOptions(ALL_ID));
					} else {
						this.addToCommandLine(getCommandLineOptions(ARCHIVE_FILES_ID));
					}
				} else {
					this.removeFromCommandLine(getCommandLineOptions(ARCHIVE_FILES_ID));
				}
				break;
			case IDialogConstants.OK_ID :
				// run button
				// build up the command line
				if (this.input != null) {
					this.addToCommandLine(getCommandLineOptions(INPUT_ID));
					this.addToCommandLine(this.input);
				}
				if (this.output != null) {
					this.addToCommandLine(getCommandLineOptions(OUTPUT_ID));
					this.addToCommandLine(this.output);
				}
				String[] cmd = new String[this.commandLine.size()];
				this.commandLine.toArray(cmd);
				for (int i = 0, max = cmd.length; i < max; i++) {
					System.out.println(cmd[i]);
				}
				try {
					Converter.main(cmd);
				} catch (RuntimeException e) {
					ApiUIPlugin.log(e);
				}
			case IDialogConstants.CANCEL_ID :
				// this is call in case of OK_ID and CANCEL_ID
				super.buttonPressed(buttonId);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#close()
	 */
	public boolean close() {
		this.dispose();
		return super.close();
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (this.title != null) {
			shell.setText(this.title);
		}
	}

	protected Control createButtonBar(Composite parent) {
		Composite composite = (Composite) super.createButtonBar(parent);
		createButton(composite, IDialogConstants.OK_ID, Messages.ConverterUI_21, true);
		createButton(composite, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		return composite;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// nothing to do
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		// set up the interface
		parent.setLayout(new GridLayout(2, false));
		Group inputGroup = new Group(parent, SWT.NORMAL);
		setInputGroup(inputGroup);
		inputGroup.setText(Messages.ConverterUI_22);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, true);
		gridData.horizontalSpan = 2;
		inputGroup.setLayoutData(gridData);
		this.widgets.add(inputGroup);
		Group outputGroup = new Group(parent, SWT.NORMAL);
		outputGroup.setText(Messages.ConverterUI_23);
		setOutputGroup(outputGroup);
		gridData = new GridData(SWT.FILL, SWT.NONE, true, true);
		gridData.horizontalSpan = 2;
		outputGroup.setLayoutData(gridData);
		this.widgets.add(outputGroup);
		Group optionsGroup = new Group(parent, SWT.NORMAL);
		optionsGroup.setText(Messages.ConverterUI_24);
		setOptionsGroup(optionsGroup);
		gridData = new GridData(SWT.FILL, SWT.NONE, true, true);
		gridData.horizontalSpan = 2;
		optionsGroup.setLayoutData(gridData);
		this.widgets.add(optionsGroup);
		return composite;
	}

	public void dispose() {
		for (Iterator iterator = this.widgets.iterator(); iterator.hasNext();) {
			Widget widget = (Widget) iterator.next();
			widget.dispose();
		}
		this.getParentShell().dispose();
	}

	public String getCommandLineOptions(int id) {
		return COMMAND_LINE_OPTIONS[id - IDialogConstants.CLIENT_ID - 1];
	}

	private void removeFromCommandLine(String argument) {
		this.commandLine.remove(argument);
	}

	private void setInputGroup(Group group) {
		GridLayout layout = new GridLayout(3, false);
		group.setLayout(layout);
		Label label = new Label(group, SWT.NORMAL);
		GC gc = new GC(label);
		gc.setFont(label.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		label.setText(Messages.ConverterUI_25);
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.widthHint = convertWidthInCharsToPixels(fontMetrics, 15);
		label.setLayoutData(data);
		this.inputText = new Text(group, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.widthHint = convertWidthInCharsToPixels(fontMetrics, 30);
		this.inputText.setLayoutData(data);
		Button button = new Button(group, SWT.PUSH);
		button.setText(Messages.ConverterUI_26);
		button.setVisible(true);
		final Button file = new Button(group, SWT.CHECK);
		file.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		file.setText(Messages.ConverterUI_27);
		file.setSelection(false);
		this.isFileInput = false;
		file.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ConverterUI.this.isFileInput = !ConverterUI.this.isFileInput;
			}
		});
		button.setData(new Integer(INPUT_ID));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				buttonPressed(((Integer) event.widget.getData()).intValue());
			}
		});
	}

	private void setKeepGroup(Group group) {
		group.setLayout(new GridLayout(2, true));
		this.addNewOption(group, KEEP_PRIVATE_ID, Messages.ConverterUI_2, false);
		this.addNewOption(group, KEEP_PROTECTED_ID, Messages.ConverterUI_1, false);
		this.addNewOption(group, KEEP_PUBLIC_ID, Messages.ConverterUI_0, false);
		this.addNewOption(group, KEEP_SYNTHETIC_ID, Messages.ConverterUI_3, false);
		this.addNewOption(group, KEEP_PACKAGE_ID, Messages.ConverterUI_4, false);
		this.addNewOption(group, KEEP_ALL_ID, Messages.ConverterUI_5, false);
		this.addNewOption(group, KEEP_NONE_ID, Messages.ConverterUI_6, false);
	}
	
	private void setOptionsGroup(Group group) {
		group.setLayout(new GridLayout(2, true));
		this.addNewOption(group, VERBOSE_ID, Messages.ConverterUI_35, false);
		this.addNewOption(group, RECURSE_ID, Messages.ConverterUI_36, false);
		this.addNewOption(group, CLASS_FILES_ID, Messages.ConverterUI_37, false);
		this.addNewOption(group, ARCHIVE_FILES_ID, Messages.ConverterUI_38, false);
		this.addNewOption(group, COMPRESS_ID, Messages.ConverterUI_39, false);
		this.addNewOption(group, REFS_ID, Messages.ConverterUI_40, false);
		this.addNewOption(group, SKIP_RESOURCE_FILES_ID, Messages.ConverterUI_41, false);
		this.addNewOption(group, ALL_ID, Messages.ConverterUI_42, false);
		Group group2 = new Group(group, SWT.NORMAL);
		group2.setText(Messages.ConverterUI_43);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		setKeepGroup(group2);
		group2.setLayoutData(gridData);
	}

	private void setOutputGroup(Group group) {
		GridLayout layout = new GridLayout(3, false);
		group.setLayout(layout);
		Label label = new Label(group, SWT.NORMAL);
		GC gc = new GC(label);
		gc.setFont(label.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		label.setText(Messages.ConverterUI_44);
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.widthHint = convertWidthInCharsToPixels(fontMetrics, 15);
		label.setLayoutData(data);
		this.outputText = new Text(group, SWT.SINGLE | SWT.BORDER);
		this.outputText.setEditable(true);
		data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.widthHint = convertWidthInCharsToPixels(fontMetrics, 30);
		this.outputText.setLayoutData(data);
		Button button = new Button(group, SWT.PUSH);
		button.setText(Messages.ConverterUI_45);
		button.setData(new Integer(OUTPUT_ID));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				buttonPressed(((Integer) event.widget.getData()).intValue());
			}
		});
	}
}
