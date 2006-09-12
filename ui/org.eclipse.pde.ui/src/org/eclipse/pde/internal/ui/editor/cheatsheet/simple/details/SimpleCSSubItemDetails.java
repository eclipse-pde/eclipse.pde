/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.SerializationException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandComposerDialog;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSElementSection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSSharedUIFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSSubItemDetails
 *
 */
public class SimpleCSSubItemDetails extends SimpleCSAbstractDetails {

	private ISimpleCSSubItem fSubItem;
	
	private FormEntry fLabel;
	
	private Button fSkip;
	
	private Section fMainSection;

	// Command Section
	
	private Section fCommandSection;
	
	private Table fCommandTable;
	
	private FormEntry fCommandEntry;		
	
	// Not supporting when at this moment; since, we are not supporting
	// conditional-subitem
	//private FormEntry fWhen;	
	
	/**
	 * @param elementSection
	 */
	public SimpleCSSubItemDetails(ISimpleCSSubItem subItem, SimpleCSElementSection elementSection) {
		super(elementSection);
		fSubItem = subItem;

		fLabel = null;
		fSkip = null;
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
		//fWhen = null;
		fMainSection = null;

		// Command Section
		fCommandSection = null;
		fCommandTable = null;
		fCommandTable = null;			
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		//Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		//Label label = null;
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;

		// Set parent layout
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		
		// Create main section
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSSubItemDetails_10);
		fMainSection.setDescription(PDEUIMessages.SimpleCSSubItemDetails_11);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);	
		layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);
		
		// Attribute: label
		fLabel = new FormEntry(mainSectionClient, toolkit, PDEUIMessages.SimpleCSSubItemDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fLabel.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fLabel.getLabel().setLayoutData(data);		

		// Attribute: skip
		fSkip = toolkit.createButton(mainSectionClient, PDEUIMessages.SimpleCSSubItemDetails_3, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);

		// Bind widgets
		toolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		createCommandSection(parent);

		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
		//fWhen = new FormEntry(optionalSectionClient, toolkit, PDEUIMessages.SimpleCSSubItemDetails_2, SWT.NONE);
		
		
	}

	/**
	 * @param parent
	 */
	public void createCommandSection(Composite parent) {

		// TODO: MP: Magic number
		// TODO: MP: Remember state of open close section		

		FormToolkit toolkit = getManagedForm().getToolkit();
		int columnSpan = 3;
		// TODO: MP: Remove painted border check - only applicable to the 
		// page not sections
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		GridLayout layout = null;		
		
		// Create command section
		fCommandSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		fCommandSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fCommandSection.marginHeight = 5;
		fCommandSection.marginWidth = 5;
		fCommandSection.setText(PDEUIMessages.SimpleCSSubItemDetails_4);
		fCommandSection.setDescription(PDEUIMessages.SimpleCSSubItemDetails_5);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fCommandSection.setLayoutData(data);
		
		// Create container for command section		
		Composite commandSectionClient = toolkit.createComposite(fCommandSection);	
		// TODO: MP: Make column magic number constant
		layout = new GridLayout(columnSpan, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		commandSectionClient.setLayout(layout);

		// Element:  command
		// For name
		fCommandEntry = new FormEntry(commandSectionClient, toolkit,
			PDEUIMessages.SimpleCSSubItemDetails_6, PDEUIMessages.GeneralInfoSection_browse, false);

		//createSpacer(commandSectionClient, toolkit, columnSpan);		
		
		//createLabel(commandSectionClient, toolkit, columnSpan, "The command parameters specify the context in which the command is executed.  The keys and values of these parameters are auto-filled in this section when a command is selected:", null);
		
		// Element: command
		// For parameters
		// Create label for the element command
		// TODO: MP: Add colo
		SimpleCSSharedUIFactory.createLabel(commandSectionClient, toolkit, columnSpan, PDEUIMessages.SimpleCSSubItemDetails_7, foreground);

		
		fCommandTable = toolkit.createTable(commandSectionClient, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 25;
		data.horizontalSpan = columnSpan;
		fCommandTable.setLayoutData(data);
		//fCommandTable.setHeaderVisible(true);
		fCommandTable.setLinesVisible(true);
		//fCommandTable.setForeground(foreground);
		TableColumn tableColumn1 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn1.setText(PDEUIMessages.SimpleCSSubItemDetails_8);
		TableColumn tableColumn2 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn2.setText(PDEUIMessages.SimpleCSSubItemDetails_9);
		
		//createSpacer(commandSectionClient, toolkit, columnSpan);			
		//createSpacer(commandSectionClient, toolkit, columnSpan);			
		
		// Bind widgets
		toolkit.paintBordersFor(commandSectionClient);
		fCommandSection.setClient(commandSectionClient);
		markDetailsPart(fCommandSection);		
		
	}
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: label
		fLabel.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// TODO: MP: Can when ever be null?
				fSubItem.setLabel(fLabel.getValue());
			}
		});	
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fSubItem.setSkip(fSkip.getSelection());
			}
		});
		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
//		fWhen.setFormEntryListener(new FormEntryAdapter(this) {
//			public void textValueChanged(FormEntry entry) {
//				// TODO: MP: Can when ever be null?
//				fSubItem.setWhen(fWhen.getValue());
//			}
//		});			
		
		hookCommandSectionListeners();
		
	}

	/**
	 * 
	 */
	private void hookCommandSectionListeners() {
		// Element: command
		fCommandEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				ISimpleCSRunContainerObject object = fSubItem.getExecutable();
				if ((object != null) && 
						(object.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
					ISimpleCSCommand command = (ISimpleCSCommand)object;
					// TODO: MP: Update when command conqueror hooked in
					command.setSerialization(fCommandEntry.getValue());
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				CommandComposerDialog dialog = new CommandComposerDialog(
						entry.getButton().getShell(),
						CommandComposerPart.F_CHEATSHEET_FILTER);	
				if (dialog.open() == Window.OK) {

					ParameterizedCommand result = dialog.getCommand();
					if (result == null) {
						return;
					}
					// TODO: Mike, do your thing
//					String serialization = result.getSerializedString();
//					String commandName = result.getCommandName();
//					HashMap parameters = result.getParameterMap();
//					if (PDETextHelper.isDefined(commandName)) {
//						fCommandEntry.setValue(commandName);
//					}
//					if (PDETextHelper.isDefined(serialization)) {
//						ISimpleCSCommand command = fSubItem.getModel().getFactory().createSimpleCSCommand(fSubItem);
//						command.setSerialization(serialization);
//						fSubItem.setExecutable(command);
//					}
//					// TODO: MP: Figure out why command is not being written back to file
//					updateCommandParameters(parameters);

				}					
			}
		});				
						
	}
	
	/**
	 * @param parameters
	 */
	private void updateCommandParameters(Map parameters) {
		if ((parameters != null) && 
				(parameters.isEmpty() == false)) {

			// TODO: MP: Add update function for table
			// TODO: MP: remove qualifyer from qualified parameter names
			// TODO: MP: Get parameterized command instead from Janek
			
			// Iterate over the keys in the map
		    Iterator it = parameters.keySet().iterator();
		    int rowCount = 0;
		    while (it.hasNext()) {
		    	TableItem item = null;
		    	if (rowCount < fCommandTable.getItemCount()) {
		    		item = fCommandTable.getItem(rowCount);
		    	} else {
		    		item = new TableItem (fCommandTable, SWT.NONE);
		    	}
		        // Get key
		        Object key = it.next();
		        if (key instanceof String) {
		        	String keyString = (String)key;
		        	int dotIndex = keyString.lastIndexOf('.');
		        	if ((dotIndex != -1) &&
		        			(dotIndex != (keyString.length() - 1))) {
		        		keyString = keyString.substring(dotIndex + 1);
		        	}
		        	item.setText(0, keyString);
		        }
		        Object value = parameters.get(key);
		        if (value instanceof String) {
		        	item.setText(1, (String)value);
		        }
		        rowCount++;
		    }

			for (int i = 0; i < fCommandTable.getColumnCount(); i++) {
				TableColumn tableColumn = fCommandTable.getColumn(i);
				tableColumn.pack();
			}						
			
		}
	}

	// TODO: MP: Refactor into a shared location
	/**
	 * @return
	 */
	private static ICommandService getCommandService() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		return (ICommandService)workbench.getAdapter(ICommandService.class);
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		
		if (fSubItem == null) {
			return;
		}
		// Attribute: label
		fLabel.setValue(fSubItem.getLabel());
		fLabel.setEditable(editable);
		
		// Attribute: skip
		fSkip.setSelection(fSubItem.getSkip());
		fSkip.setEnabled(editable);
		
		// Attribute: when
		// Not supporting when at this moment; since, we are not supporting
		// conditional-subitem
//		fWhen.setValue(fSubItem.getWhen(), true);
//		fWhen.setEditable(editable);
		
		updateCommandSectionFields();
		
		// TODO: MP: Add update function for table
		// TODO: MP: remove qualifyer from qualified parameter names

		
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSAbstractDetails#updateFields()
	 */
	private void updateCommandSectionFields() {

		boolean editable = isEditableElement();
		
		if (fSubItem == null) {
			return;
		}

		ISimpleCSRunContainerObject object = fSubItem.getExecutable();
		if ((object != null) && 
				(object.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
			ISimpleCSCommand command = (ISimpleCSCommand)object;
			String serialization = command.getSerialization();
			if (PDETextHelper.isDefined(serialization)) {
				
				ICommandService service = getCommandService();
				if (service != null) {
					try {
						ParameterizedCommand parameterizedCommand = service.deserialize(serialization);
						fCommandEntry.setValue(parameterizedCommand.getCommand().getName(), true);
						Map parameters = parameterizedCommand.getParameterMap();
						updateCommandParameters(parameters);
						
					} catch (NotDefinedException e) {
						// TODO: MP: Auto-generated catch block
						e.printStackTrace();
					} catch (SerializationException e) {
						// TODO: MP:  Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
		fCommandEntry.setEditable(editable);
		fCommandEntry.getText().setEditable(false);	
		
		fCommandTable.setEnabled(editable);
		
	}	

}
