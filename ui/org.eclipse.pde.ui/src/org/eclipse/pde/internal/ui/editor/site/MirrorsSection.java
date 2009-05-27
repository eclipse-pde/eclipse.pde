/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;

/**
 * 
 */
public class MirrorsSection extends PDESection {
	private FormEntry fMirrorsURLEntry;

	public MirrorsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.TWISTIE);
		getSection().setText(PDEUIMessages.SiteEditor_MirrorsSection_header);
		getSection().setDescription(PDEUIMessages.SiteEditor_MirrorsSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fMirrorsURLEntry.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(data);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 200;
		section.setLayoutData(data);

		fMirrorsURLEntry = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_MirrorsSection_urlLabel, null, false);
		fMirrorsURLEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setMirrorsURL(text.getValue());
			}
		});
		fMirrorsURLEntry.setEditable(isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private void setMirrorsURL(String text) {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		try {
			site.setMirrorsURL(text);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		refresh();
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	public void setFocus() {
		if (fMirrorsURLEntry != null)
			fMirrorsURLEntry.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		setIfDefined(fMirrorsURLEntry, site.getMirrorsURL());
		super.refresh();
	}

	public void cancelEdit() {
		fMirrorsURLEntry.cancelEdit();
		super.cancelEdit();
	}

	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
}
