/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.SerializationException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRun;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractSubDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.commands.CommandComposerDialog;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;

public class SimpleCSCommandDetails extends CSAbstractSubDetails {

	private ISimpleCSRun fRun;

	private Table fCommandTable;

	private SimpleCSCommandComboPart fCommandCombo;

	private ControlDecoration fCommandInfoDecoration;

	private Button fCommandBrowse;

	private Button fCommandOptional;

	private static final String F_NO_COMMAND = SimpleDetailsMessages.SimpleCSCommandDetails_none;

	private static final int F_COMMAND_INSERTION_INDEX = 1;

	/**
	 * @param section
	 */
	public SimpleCSCommandDetails(ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fRun = null;

		fCommandTable = null;
		fCommandCombo = null;
		fCommandInfoDecoration = null;
		fCommandBrowse = null;
		fCommandOptional = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#setData(java.lang.Object)
	 */
	public void setData(ISimpleCSRun object) {
		// Set data
		fRun = object;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		int columnSpan = 3;
		Section commandSection = null;
		FormToolkit toolkit = getToolkit();
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);
		GridData data = null;
		Label label = null;

		// Create command section
		commandSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		commandSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		commandSection.setText(SimpleDetailsMessages.SimpleCSCommandDetails_commandSectionText);
		commandSection.setDescription(SimpleDetailsMessages.SimpleCSCommandDetails_commandSectionDesc);
		commandSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		commandSection.setLayoutData(data);

		// Create container for command section		
		Composite commandSectionClient = toolkit.createComposite(commandSection);
		commandSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, columnSpan));

		// Element:  command
		// Label
		label = toolkit.createLabel(commandSectionClient, SimpleDetailsMessages.SimpleCSCommandDetails_attrCommand, SWT.WRAP);
		label.setForeground(foreground);
		// Combo box
		fCommandCombo = new SimpleCSCommandComboPart();
		fCommandCombo.createControl(commandSectionClient, toolkit, SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fCommandCombo.getControl().setLayoutData(data);
		// Insertion index is 0 for no command combo box entry
		// Always keep this entry as the first entry
		fCommandCombo.add(F_NO_COMMAND);
		fCommandCombo.setText(F_NO_COMMAND);
		fCommandCombo.populate();
		// Always insert new command keys obtained from other combo boxes in 
		// the position after the no command entry
		fCommandCombo.setNewCommandKeyIndex(F_COMMAND_INSERTION_INDEX);
		// Limit the combo box to the 11 most recent entries (includes no 
		// command entry)
		fCommandCombo.setComboEntryLimit(11);

		createCommandInfoDecoration();
		// Button
		fCommandBrowse = toolkit.createButton(commandSectionClient, SimpleDetailsMessages.SimpleCSCommandDetails_browse, SWT.PUSH);

		// Element: command
		// Label for parameters
		label = toolkit.createLabel(commandSectionClient, SimpleDetailsMessages.SimpleCSCommandDetails_attrParameters, SWT.WRAP);
		label.setForeground(foreground);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = columnSpan;
		label.setLayoutData(data);

		fCommandTable = toolkit.createTable(commandSectionClient, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 25;
		data.horizontalSpan = columnSpan;
		fCommandTable.setLayoutData(data);
		//fCommandTable.setHeaderVisible(true);
		fCommandTable.setLinesVisible(true);
		//fCommandTable.setForeground(foreground);
		TableColumn tableColumn1 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn1.setText(SimpleDetailsMessages.SimpleCSCommandDetails_name);
		TableColumn tableColumn2 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn2.setText(SimpleDetailsMessages.SimpleCSCommandDetails_value);

		// Attribute: required
		fCommandOptional = getToolkit().createButton(commandSectionClient, SimpleDetailsMessages.SimpleCSCommandDetails_attrOptional, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = columnSpan;
		fCommandOptional.setLayoutData(data);
		fCommandOptional.setForeground(foreground);

		// Bind widgets
		toolkit.paintBordersFor(commandSectionClient);
		commandSection.setClient(commandSectionClient);
		// Mark as a details part to enable cut, copy, paste, etc.
		markDetailsPart(commandSection);
	}

	/**
	 * @param label
	 */
	private void createCommandInfoDecoration() {
		// Command info decoration
		int bits = SWT.TOP | SWT.LEFT;
		fCommandInfoDecoration = new ControlDecoration(fCommandCombo.getControl(), bits);
		fCommandInfoDecoration.setMarginWidth(1);
		fCommandInfoDecoration.setDescriptionText(SimpleDetailsMessages.SimpleCSCommandDetails_disabled);
		updateCommandInfoDecoration(false);
		fCommandInfoDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#hookListeners()
	 */
	public void hookListeners() {

		// Element: command
		fCommandCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fRun == null) {
					return;
				}
				String selection = fCommandCombo.getSelection();
				if (selection.equals(F_NO_COMMAND) == false) {
					// Get the associated serialization stored as data against the 
					// command name
					String serialization = fCommandCombo.getValue(selection);
					if (PDETextHelper.isDefined(serialization)) {
						// Create the new command in the model
						createCommandInModel(serialization);

						ParameterizedCommand result = getParameterizedCommand(serialization);
						if (result != null) {
							updateCommandTable(result.getParameterMap());
						}
					}
				} else {
					// The empty entry was selected
					// Delete the existing command
					fRun.setExecutable(null);
					fCommandTable.clearAll();
				}
				// Update the master section buttons
				getMasterSection().updateButtons();
				// Update the optional command checkbox
				updateUICommandOptional();
			}
		});

		fCommandBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fRun == null) {
					return;
				}
				// Open the command composer dialog using the input from the
				// currently selected command
				CommandComposerDialog dialog = new CommandComposerDialog(fCommandBrowse.getShell(), CommandComposerPart.F_CHEATSHEET_FILTER, getParameterizedCommand(fRun), getSnapshotContext());
				// Check result of dialog
				if (dialog.open() == Window.OK) {
					// Command composer exited successfully
					// Update accordingly
					updateCommandCombo(dialog.getCommand(), true);
					// Update the master section buttons
					getMasterSection().updateButtons();
					// Update the optional command checkbox
					updateUICommandOptional();
				}
			}
		});

		// Attribute: required
		fCommandOptional.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fRun == null) {
					return;
				}
				// Get the command
				ISimpleCSCommand commandObject = getCommandObject(fRun);
				// Ensure the command is defined
				if (commandObject == null) {
					return;
				}
				// Set required value in model
				boolean isRequired = (fCommandOptional.getSelection() == false);
				commandObject.setRequired(isRequired);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fRun == null) {
			return;
		}
		// i.e. Action: class
		ParameterizedCommand command = getParameterizedCommand(fRun);
		if (command == null) {
			// Since, this page is static the command combo and command table
			// must be reset
			clearCommandUI();
		} else {
			updateCommandCombo(command, false);
		}
		// Update the optional command checkbox
		updateUICommandOptional();
		// Update command UI enablement
		updateCommandEnablement();
	}

	/**
	 * 
	 */
	private void updateUICommandOptional() {
		// Attribute: required
		ISimpleCSCommand commandObject = getCommandObject(fRun);
		if (commandObject == null) {
			fCommandOptional.setSelection(false);
			fCommandOptional.setEnabled(false);
		} else {
			boolean isOptional = (commandObject.getRequired() == false);
			fCommandOptional.setSelection(isOptional);
			fCommandOptional.setEnabled(isEditableElement());
		}
	}

	/**
	 * @param runObject
	 * @return
	 */
	private ISimpleCSCommand getCommandObject(ISimpleCSRun runObject) {
		// Ensure the run object is defined
		if (runObject == null) {
			return null;
		}
		// Get the executable
		ISimpleCSRunContainerObject executable = runObject.getExecutable();
		// Ensure executable is defined
		if (executable == null) {
			return null;
		} else if (executable.getType() != ISimpleCSConstants.TYPE_COMMAND) {
			// Not a command
			return null;
		}
		return (ISimpleCSCommand) executable;
	}

	/**
	 * 
	 */
	private void clearCommandUI() {
		// Clear the command combo
		fCommandCombo.setText(F_NO_COMMAND);
		// Clear the command table
		fCommandTable.clearAll();
	}

	/**
	 * 
	 */
	private void updateCommandEnablement() {
		// Ensure data object is defined
		if (fRun == null) {
			return;
		}
		boolean editable = isEditableElement();

		if (fRun.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem item = (ISimpleCSItem) fRun;
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot have a subitem and any of the following
			// together:  perform-when, command, action			
			if (item.hasSubItems()) {
				editable = false;
				updateCommandInfoDecoration(true);
			} else {
				updateCommandInfoDecoration(false);
			}
		}

		fCommandCombo.setEnabled(editable);
		fCommandTable.setEnabled(true);
		fCommandBrowse.setEnabled(editable);
	}

	/**
	 * @param serialization
	 */
	private void createCommandInModel(String serialization) {
		// Ensure data object is defined
		if (fRun == null) {
			return;
		}
		ISimpleCSCommand command = fRun.getModel().getFactory().createSimpleCSCommand(fRun);
		command.setSerialization(serialization);
		command.setRequired(false);
		fRun.setExecutable(command);
	}

	/**
	 * @param result
	 * @param createInModel
	 */
	private void updateCommandCombo(ParameterizedCommand result, boolean createInModel) {

		if (result == null) {
			return;
		}
		// Get serialization
		String serialization = result.serialize();
		// Get presentable command name
		String commandName = null;
		try {
			commandName = result.getCommand().getName();
		} catch (NotDefinedException e) {
			// Ignore, name will be undefined
		}
		// Get command ID
		String commandId = result.getId();

		if (PDETextHelper.isDefined(serialization) && PDETextHelper.isDefined(commandId)) {
			if (createInModel) {
				// Create the new command in the model
				createCommandInModel(serialization);
			}
			// Determine the presentable name to use in the combo box and the
			// key to store the serialization data against in the widget
			String nameToUse = null;
			if (PDETextHelper.isDefined(commandName)) {
				nameToUse = commandName;
			} else {
				nameToUse = commandId;
			}
			// Add new selection to the combo box if it is not already there
			// Associate the serialization with the command name
			// in the widget to retrieve for later use
			fCommandCombo.putValue(nameToUse, serialization, F_COMMAND_INSERTION_INDEX);
			// Select it
			fCommandCombo.setText(nameToUse);
			// Update the command table parameters
			updateCommandTable(result.getParameterMap());
		} else {
			// No serialization, something bad happened
			fCommandCombo.setText(F_NO_COMMAND);
		}

	}

	/**
	 * @param serialization
	 * @return
	 */
	private ParameterizedCommand getParameterizedCommand(String serialization) {
		if (PDETextHelper.isDefined(serialization)) {
			ICommandService service = getCommandService();
			if (service != null) {
				try {
					return service.deserialize(serialization);
				} catch (NotDefinedException e) {
					PDEUserAssistanceUIPlugin.logException(e, SimpleDetailsMessages.SimpleCSCommandDetails_errTitle, SimpleDetailsMessages.SimpleCSCommandDetails_errMsg + serialization);
				} catch (SerializationException e) {
					PDEUserAssistanceUIPlugin.logException(e, SimpleDetailsMessages.SimpleCSCommandDetails_errTitle, SimpleDetailsMessages.SimpleCSCommandDetails_errMsg + serialization);
				}
			}
		}
		return null;
	}

	/**
	 * @param run
	 * @return
	 */
	private ParameterizedCommand getParameterizedCommand(ISimpleCSRun run) {
		if (run == null) {
			return null;
		}
		ISimpleCSRunContainerObject object = run.getExecutable();
		if ((object != null) && (object.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
			ISimpleCSCommand command = (ISimpleCSCommand) object;
			return getParameterizedCommand(command.getSerialization());
		}
		return null;
	}

	/**
	 * @param parameters
	 */
	private void updateCommandTable(Map parameters) {
		// Clear the table contents
		fCommandTable.clearAll();

		if ((parameters != null) && (parameters.isEmpty() == false)) {
			// Iterate over the keys in the map
			Iterator it = parameters.keySet().iterator();
			int rowCount = 0;
			while (it.hasNext()) {
				// Track number of keys / rows processed
				TableItem item = null;
				// Determine if there is an existing row already at that index
				if (rowCount < fCommandTable.getItemCount()) {
					// There is, reuse it
					item = fCommandTable.getItem(rowCount);
				} else {
					// There isn't, create a new one
					item = new TableItem(fCommandTable, SWT.NONE);
				}
				// Get key
				Object key = it.next();
				if (key instanceof String) {
					String keyString = (String) key;
					// If present, remove the fully qualified ID from the
					// paramater key
					// i.e. "org.eclipse.ui.perspective" becomes just 
					// "perspective" 
					int dotIndex = keyString.lastIndexOf('.');
					if ((dotIndex != -1) && (dotIndex != (keyString.length() - 1))) {
						keyString = keyString.substring(dotIndex + 1);
					}
					// Set parameter key in first column
					item.setText(0, keyString);
				}
				Object value = parameters.get(key);
				if (value instanceof String) {
					// Set parameter value in second column
					item.setText(1, (String) value);
				}
				rowCount++;
			}
			// Pack the columns with the new data
			for (int i = 0; i < fCommandTable.getColumnCount(); i++) {
				TableColumn tableColumn = fCommandTable.getColumn(i);
				tableColumn.pack();
			}
		}
	}

	private static ICommandService getCommandService() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		return (ICommandService) workbench.getAdapter(ICommandService.class);
	}

	private static IHandlerService getGlobalHandlerService() {
		return (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
	}

	private static IEvaluationContext getSnapshotContext() {
		IHandlerService service = getGlobalHandlerService();
		return service.createContextSnapshot(false);
	}

	/**
	 * 
	 */
	private void updateCommandInfoDecoration(boolean showDecoration) {
		if (showDecoration) {
			fCommandInfoDecoration.show();
		} else {
			fCommandInfoDecoration.hide();
		}
		fCommandInfoDecoration.setShowHover(showDecoration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// NO-OP
		// No form entries
	}
}
