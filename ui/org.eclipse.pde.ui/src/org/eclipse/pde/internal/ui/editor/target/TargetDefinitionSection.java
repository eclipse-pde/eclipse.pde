/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class TargetDefinitionSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fIDEntry;
	private FormEntry fPath;
	private Button fUseDefault;
	private Button fCustomPath;
	private Button fFileSystem;
	private Button fVariable;
	private static int NUM_COLUMNS = 5;
	
	public TargetDefinitionSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TITLE_BAR|Section.TWISTIE);
		createClient(getSection(), page.getEditor().getToolkit());
		getSection().setExpanded(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = NUM_COLUMNS ;
		client.setLayout(layout);

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		createNameEntry(client, toolkit, actionBars);
		
		createIDEntry(client, toolkit, actionBars);
		
		createLocation(client, toolkit, actionBars);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText(PDEUIMessages.TargetDefinitionSection_title); 
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_name, null, false); 
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getTarget().setName(entry.getValue());
			}
		});
		GridData gd = (GridData)fNameEntry.getText().getLayoutData();
		gd.horizontalSpan = 4;
		fNameEntry.setEditable(isEditable());
	}

	private void createIDEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIDEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_id, null, false); 
		fIDEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getTarget().setId(entry.getValue());
			}
		});
		GridData gd = (GridData)fIDEntry.getText().getLayoutData();
		gd.horizontalSpan = 4;
		fIDEntry.setEditable(isEditable());
	}
	
	private void createLocation(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		Label label = toolkit.createLabel(client, "Target location:");
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fUseDefault = toolkit.createButton(client, "The target platform is the same as the host (running) platform", SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 5;
		gd.horizontalIndent = 15;
		fUseDefault.setLayoutData(gd);
		fUseDefault.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fUseDefault.getSelection()) {
					fPath.getText().setEnabled(false);
					fFileSystem.setEnabled(false);
					fVariable.setEnabled(false);
					String path = fPath.getValue();
					if (path.length() > 0)
						getLocationInfo().setPath("");
				}
			}
		});
		
		fCustomPath = toolkit.createButton(client, "Location:", SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 15;
		fCustomPath.setLayoutData(gd);
		fCustomPath.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fCustomPath.getSelection()) {
					fPath.getText().setEnabled(true);
					fFileSystem.setEnabled(true);
					fVariable.setEnabled(true);
					String path = fPath.getValue();
					if (path.length() > 0)
						getLocationInfo().setPath(path);
				}
			}
		});
		
		fPath = new FormEntry(client, toolkit, null, null, false); 
		fPath.getText().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPath.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLocationInfo().setPath(fPath.getValue());
			}
		});
		
		fFileSystem = toolkit.createButton(client, "File System...", SWT.PUSH);
		fFileSystem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFileSystem();
			}
		});
		
		fVariable = toolkit.createButton(client, "Variables...", SWT.PUSH);
		fVariable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInsertVariable();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fIDEntry.commit();
		fPath.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fIDEntry.cancelEdit();
		fPath.cancelEdit();
		super.cancelEdit();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ITarget target = getTarget();
		fNameEntry.setValue(target.getName(), true);
		fIDEntry.setValue(target.getId(), true);
		ILocationInfo info = getLocationInfo();
		fUseDefault.setSelection(info.useDefault());
		fCustomPath.setSelection(!info.useDefault());
		fPath.setValue(info.getPath(),true);
		fPath.getText().setEnabled(!info.useDefault());
		fFileSystem.setEnabled(!info.useDefault());
		fVariable.setEnabled(!info.useDefault());
		super.refresh();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
	
	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(getSection().getShell());
		dialog.setFilterPath(fPath.getValue());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null) {
			fPath.setValue(result);
			getLocationInfo().setPath(result);
		}
	}
	
	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
					new StringVariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell());
		if (dialog.open() == StringVariableSelectionDialog.OK) {
			fPath.getText().insert(dialog.getVariableExpression());
			// have to setValue to make sure getValue reflects the actual text in the Text object.
			fPath.setValue(fPath.getText().getText());
			getLocationInfo().setPath(fPath.getText().getText());
		}
	}
	
	private ILocationInfo getLocationInfo() {
		ILocationInfo info = getTarget().getLocationInfo();
		if (info == null) {
			info = getModel().getFactory().createLocation();
			getTarget().setLocationInfo(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
}
