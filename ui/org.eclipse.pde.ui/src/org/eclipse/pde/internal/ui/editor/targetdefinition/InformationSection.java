/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing target name and description in the target definition editor
 * @see DefinitionPage
 * @see TargetEditor
 */
public class InformationSection extends SectionPart {

	private FormEntry fNameEntry;
	private FormEntry fDescEntry;
	private TargetEditor fEditor;

	public InformationSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 * 
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		section.setText(PDEUIMessages.InformationSection_0);
		section.setDescription(PDEUIMessages.InformationSection_1);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.TargetDefinitionSection_name, null, false);
		fNameEntry.setValue(getTarget().getName());
		fNameEntry.setFormEntryListener(new SimpleFormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String value = entry.getValue();
				getTarget().setName(value.length() > 0 ? value : null);
			}
		});

		fDescEntry = new FormEntry(client, toolkit, PDEUIMessages.InformationSection_2, null, false);
		fDescEntry.setValue(getTarget().getName());
		fDescEntry.setFormEntryListener(new SimpleFormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String value = entry.getValue();
				getTarget().setDescription(value.length() > 0 ? value : null);
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fDescEntry.commit();
		super.commit(onSave);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fNameEntry.setValue(getTarget().getName(), true);
		fDescEntry.setValue(getTarget().getDescription(), true);
		super.refresh();
	}

}
