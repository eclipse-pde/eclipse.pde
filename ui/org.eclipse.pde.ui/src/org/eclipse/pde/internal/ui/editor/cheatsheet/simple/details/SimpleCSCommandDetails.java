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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRun;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandComposerDialog;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
 * SimpleCSCommandDetailsSection
 *
 */
public class SimpleCSCommandDetails implements ISimpleCSDetails {

	private ISimpleCSRun fRun;

	private SimpleCSAbstractDetails fDetails;	
	
	private Table fCommandTable;
	
	private FormEntry fCommandEntry;		
	
	/**
	 * 
	 */
	public SimpleCSCommandDetails(ISimpleCSRun run,
			SimpleCSAbstractDetails details) {
		fRun = run;
		fDetails = details;		

		fCommandTable = null;
		fCommandEntry = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		// TODO: MP: Remember state of open close section		

		int columnSpan = 3;
		Section commandSection = null;
		FormToolkit toolkit = fDetails.getToolkit();
		// TODO: MP: Remove painted border check - only applicable to the 
		// page not sections
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		GridLayout layout = null;		
		
		// Create command section
		commandSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		//| ExpandableComposite.TWISTIE
		commandSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		commandSection.marginHeight = 5;
		commandSection.marginWidth = 5;
		commandSection.setText(PDEUIMessages.SimpleCSItemDetails_5);
		commandSection.setDescription(PDEUIMessages.SimpleCSItemDetails_6);
		data = new GridData(GridData.FILL_HORIZONTAL);
		commandSection.setLayoutData(data);
		
		// Create container for command section		
		Composite commandSectionClient = toolkit.createComposite(commandSection);	
		// TODO: MP: Make column magic number constant
		layout = new GridLayout(columnSpan, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		commandSectionClient.setLayout(layout);

		// Element:  command
		// For name
		fCommandEntry = new FormEntry(commandSectionClient, toolkit,
			PDEUIMessages.SimpleCSItemDetails_7, PDEUIMessages.GeneralInfoSection_browse, false);

		// Element: command
		// For parameters
		// Create label for the element command
		// TODO: MP: Add colo
		fDetails.createLabel(commandSectionClient, toolkit, columnSpan, PDEUIMessages.SimpleCSItemDetails_8, foreground);

		
		fCommandTable = toolkit.createTable(commandSectionClient, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 25;
		data.horizontalSpan = columnSpan;
		fCommandTable.setLayoutData(data);
		//fCommandTable.setHeaderVisible(true);
		fCommandTable.setLinesVisible(true);
		//fCommandTable.setForeground(foreground);
		TableColumn tableColumn1 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn1.setText(PDEUIMessages.SimpleCSItemDetails_9);
		TableColumn tableColumn2 = new TableColumn(fCommandTable, SWT.LEFT);
		tableColumn2.setText(PDEUIMessages.SimpleCSItemDetails_10);
		
		//createSpacer(commandSectionClient, toolkit, columnSpan);			
		//createSpacer(commandSectionClient, toolkit, columnSpan);			
		
		// Bind widgets
		toolkit.paintBordersFor(commandSectionClient);
		commandSection.setClient(commandSectionClient);
		// TODO: MP: Need to do anything here?
		//markDetailsPart(fCommandSection);	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#hookListeners()
	 */
	public void hookListeners() {
		// Element: command
		fCommandEntry.setFormEntryListener(new FormEntryAdapter(fDetails) {
			public void textValueChanged(FormEntry entry) {
				ISimpleCSRunContainerObject object = fRun.getExecutable();
				if ((object != null) && 
						(object.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
					ISimpleCSCommand command = (ISimpleCSCommand)object;
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

					try {
						String serialization = result.serialize();
						String commandName = result.getCommand().getName();
						Map parameters = result.getParameterMap();
						if (PDETextHelper.isDefined(commandName)) {
							fCommandEntry.setValue(commandName);
						}
						if (PDETextHelper.isDefined(serialization)) {
							ISimpleCSCommand command = fRun.getModel()
									.getFactory().createSimpleCSCommand(fRun);
							command.setSerialization(serialization);
							fRun.setExecutable(command);
						}
						updateCommandParameters(parameters);
					} catch (NotDefinedException e) {
						// TODO: MP: Auto-generated catch block
						e.printStackTrace();
					}

				}					
			}
		});				
				
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails#updateFields()
	 */
	public void updateFields() {
		
		boolean editable = fDetails.isEditableElement();
		
		if (fRun == null) {
			return;
		}

		ISimpleCSRunContainerObject object = fRun.getExecutable();
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

	/**
	 * @param parameters
	 */
	private void updateCommandParameters(Map parameters) {
		
		fCommandTable.clearAll();
		
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

	/**
	 * @return
	 */
	private static ICommandService getCommandService() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		return (ICommandService)workbench.getAdapter(ICommandService.class);
	}	
	
}
