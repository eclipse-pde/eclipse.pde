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

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class TargetDefinitionSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fIDEntry;
	private static int NUM_COLUMNS = 2;
	
	public TargetDefinitionSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TITLE_BAR);
		createClient(getSection(), page.getEditor().getToolkit());
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
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText(PDEUIMessages.TargetDefinitionSection_title); 
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_name, null, false); 
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fNameEntry.setEditable(isEditable());
	}

	private void createIDEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIDEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_id, null, false); 
		fIDEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		fIDEntry.setEditable(isEditable());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fIDEntry.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fIDEntry.cancelEdit();
		super.cancelEdit();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fNameEntry.setValue("", true); //$NON-NLS-1$
		fIDEntry.setValue("", true); //$NON-NLS-1$
		super.refresh();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
}
