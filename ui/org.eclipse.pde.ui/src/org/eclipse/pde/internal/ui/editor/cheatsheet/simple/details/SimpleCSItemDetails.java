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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSItemDetails
 *
 */
public class SimpleCSItemDetails extends SimpleCSAbstractDetails implements
		ISimpleCSHelpDetails {

	private ISimpleCSItem fItem;
	
	private FormEntry fTitle;
	
	private Button fDialog;
	
	private Button fSkip;	
	
	private Text fContextId;
	
	private Text fHref;
	
	private FormEntry fContent;

	private Section fMainSection;	
	
	//private Section fCommandSection;

	private Section fHelpSection;

	private Button fContextIdRadio;	

	private Button fHrefRadio;	
	
	// Command Section
	
	private Section fCommandSection;
	
	private Table fCommandTable;
	
	private FormEntry fCommandEntry;	
	
	
	/**
	 * 
	 */
	public SimpleCSItemDetails(ISimpleCSItem item, SimpleCSElementSection section) {
		super(section);
		fItem = item;
		
		fTitle = null;
		fDialog = null;
		fSkip = null;
		fContextId = null;
		fHref = null;
		fContent = null;
		fMainSection = null;
		//fCommandSection = null;
		fHelpSection = null;
		fContextIdRadio = null;
		fHrefRadio = null;
		
		// Command Section
		fCommandSection = null;
		fCommandTable = null;
		fCommandTable = null;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// TODO: MP: Probably can refactor this back into super class as utility
		// Creation of section and composite
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		GridData data = null;
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
//		Label label = null;
		
		// Set parent layout
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		parent.setLayout(layout);
		
		// Create main section
		// TODO: MP: Do make section scrollable
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fMainSection.marginHeight = 5;
		fMainSection.marginWidth = 5; 
		fMainSection.setText(PDEUIMessages.SimpleCSItemDetails_11);
		fMainSection.setDescription(PDEUIMessages.SimpleCSItemDetails_12);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		
		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);	
		layout = new GridLayout(2, false);
		if (paintedBorder) {
			layout.verticalSpacing = 7;
		}
		mainSectionClient.setLayout(layout);				

		// Attribute: title
		fTitle = new FormEntry(mainSectionClient, toolkit, PDEUIMessages.SimpleCSItemDetails_0, SWT.NONE);

		// description: Content (Element)
		fContent = new FormEntry(mainSectionClient, toolkit, PDEUIMessages.SimpleCSDescriptionDetails_0, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 90;
		//data.horizontalSpan = 2;
		fContent.getText().setLayoutData(data);	
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fContent.getLabel().setLayoutData(data);		

		// Attribute: dialog
		fDialog = toolkit.createButton(mainSectionClient, PDEUIMessages.SimpleCSItemDetails_13, SWT.CHECK);
														
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fDialog.setLayoutData(data);
		fDialog.setForeground(foreground);
		
		// Attribute: skip
		fSkip = toolkit.createButton(mainSectionClient, PDEUIMessages.SimpleCSItemDetails_14, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
		
		// Bind widgets
		toolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);		
		
		createCommandSection(parent);
		
		// TODO: MP: Magic number
		// TODO: MP: Remember state of open close section		
		fHelpSection = SimpleCSSharedUIFactory.createHelpSection(parent,
				toolkit, 1, this);
		if (fHelpSection == null) {}

		
		
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
		fCommandSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		//| ExpandableComposite.TWISTIE
		fCommandSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fCommandSection.marginHeight = 5;
		fCommandSection.marginWidth = 5;
		fCommandSection.setText(PDEUIMessages.SimpleCSItemDetails_5);
		fCommandSection.setDescription(PDEUIMessages.SimpleCSItemDetails_6);
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
			PDEUIMessages.SimpleCSItemDetails_7, PDEUIMessages.GeneralInfoSection_browse, false);

		//createSpacer(commandSectionClient, toolkit, columnSpan);		
		
		//createLabel(commandSectionClient, toolkit, columnSpan, "The command parameters specify the context in which the command is executed.  The keys and values of these parameters are auto-filled in this section when a command is selected:", null);
		
		// Element: command
		// For parameters
		// Create label for the element command
		// TODO: MP: Add colo
		SimpleCSSharedUIFactory.createLabel(commandSectionClient, toolkit, columnSpan, PDEUIMessages.SimpleCSItemDetails_8, foreground);

		
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
		fCommandSection.setClient(commandSectionClient);
		markDetailsPart(fCommandSection);		
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {

		// description: Content (Element)
		fContent.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fItem.getDescription() != null) {
					fItem.getDescription().setContent(fContent.getValue());
				}
			}
		});		
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fItem.setTitle(fTitle.getValue());
			}
		});
		// Attribute: dialog
		fDialog.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setDialog(fDialog.getSelection());
			}
		});	
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fItem.setSkip(fSkip.getSelection());
			}
		});	
		// Attribute: contextId
		fContextId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fItem.setContextId(fContextId.getText());
			}
		});
		// Attribute: href
		fHref.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fItem.setHref(fHref.getText());
			}
		});	
		// Radio button for contextId
		fContextIdRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fContextIdRadio.getSelection();
				fContextId.setEnabled(selected);
				fHref.setEnabled(!selected);				
			}
		});		
		
		hookCommandSectionListeners();
		
	}

	/**
	 * 
	 */
	private void hookCommandSectionListeners() {
		// Element: command
		fCommandEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				ISimpleCSRunContainerObject object = fItem.getExecutable();
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

					try {
						String serialization = result.serialize();
						String commandName = result.getCommand().getName();
						Map parameters = result.getParameterMap();
						if (PDETextHelper.isDefined(commandName)) {
							fCommandEntry.setValue(commandName);
						}
						if (PDETextHelper.isDefined(serialization)) {
							ISimpleCSCommand command = fItem.getModel()
									.getFactory().createSimpleCSCommand(fItem);
							command.setSerialization(serialization);
							fItem.setExecutable(command);
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
		
		if (fItem == null) {
			return;
		}
		// TODO: MP: Check isdefined for all parameters in updateFields methods
		// Attribute: title
		if (PDETextHelper.isDefined(fItem.getTitle())) {
			fTitle.setValue(fItem.getTitle(), true);
		}
		fTitle.setEditable(editable);

		// Attribute: dialog
		fDialog.setSelection(fItem.getDialog());
		fDialog.setEnabled(editable);
		
		// Attribute: skip
		fSkip.setSelection(fItem.getSkip());
		fSkip.setEnabled(editable);
		
		// Attribute: contextId
		// Attribute: href		
		// Radio button for contextId
		// Radio button for contextId		
		if (PDETextHelper.isDefined(fItem.getContextId())) {
			fContextId.setText(fItem.getContextId());
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);			
		} else if (PDETextHelper.isDefined(fItem.getHref())) {
			fHref.setText(fItem.getHref());
			fContextId.setEnabled(false);
			fContextIdRadio.setSelection(false);			
			fHref.setEnabled(true && editable);			
			fHrefRadio.setSelection(true && editable);
		} else {
			fContextId.setEnabled(true && editable);
			fContextIdRadio.setSelection(true && editable);
			fHref.setEnabled(false);	
			fHrefRadio.setSelection(false);					
		}


		updateCommandSectionFields();
		
		// TODO: MP: Important: revist all parameters and check we are simply
		// looking for null - okay for non-String types
		// TODO: MP: Reevaluate write methods and make sure not writing empty string
		
		
		if (fItem.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		if (PDETextHelper.isDefined(fItem.getDescription().getContent())) {
			fContent.setValue(fItem.getDescription().getContent());
		}
		fContent.setEditable(editable);			

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.SimpleCSAbstractDetails#updateFields()
	 */
	private void updateCommandSectionFields() {

		boolean editable = isEditableElement();
		
		if (fItem == null) {
			return;
		}

		ISimpleCSRunContainerObject object = fItem.getExecutable();
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
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setContextId(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void setContextId(Text contextId) {
		fContextId = contextId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setContextIdRadio(org.eclipse.swt.widgets.Button)
	 */
	public void setContextIdRadio(Button contextIdRadio) {
		fContextIdRadio = contextIdRadio;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setHref(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void setHref(Text href) {
		fHref = href;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSHelpDetails#setHrefRadio(org.eclipse.swt.widgets.Button)
	 */
	public void setHrefRadio(Button hrefRadio) {
		fHrefRadio = hrefRadio;
	}
	
}
