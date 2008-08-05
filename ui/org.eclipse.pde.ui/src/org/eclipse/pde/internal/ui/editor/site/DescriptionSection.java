/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * 
 */
public class DescriptionSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fURLEntry;
	private FormEntry fDescEntry;

	public DescriptionSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.SiteEditor_DescriptionSection_header);
		getSection().setDescription(PDEUIMessages.SiteEditor_DescriptionSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fNameEntry.commit();
		fURLEntry.commit();
		fDescEntry.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		fNameEntry = new FormEntry(container, toolkit, PDEUIMessages.DescriptionSection_nameLabel, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setName(text.getValue());
			}
		});
		fNameEntry.setEditable(isEditable());

		fURLEntry = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_DescriptionSection_urlLabel, null, false);
		fURLEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setDescriptionURL(text.getValue());
			}
		});
		fURLEntry.setEditable(isEditable());

		fDescEntry = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_DescriptionSection_descLabel, SWT.WRAP | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		gd.heightHint = 64;
		fDescEntry.getText().setLayoutData(gd);
		fDescEntry.getLabel().setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fDescEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setDescriptionText(text.getValue());
			}
		});
		fDescEntry.setEditable(isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private void setName(String text) {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		ISiteDescription description = site.getDescription();
		boolean defined = false;
		if (description == null) {
			description = model.getFactory().createDescription(null);
			defined = true;
		}
		try {
			description.setName(text);
			if (defined) {
				site.setDescription(description);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setDescriptionURL(String text) {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		ISiteDescription description = site.getDescription();
		boolean defined = false;
		if (description == null) {
			description = model.getFactory().createDescription(null);
			defined = true;
		}
		try {
			description.setURL(text);
			if (defined) {
				site.setDescription(description);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setDescriptionText(String text) {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		ISiteDescription description = site.getDescription();
		boolean defined = false;
		if (description == null) {
			description = model.getFactory().createDescription(null);
			defined = true;
		}
		try {
			description.setText(text);
			if (defined) {
				site.setDescription(description);
			}
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
		if (fNameEntry != null)
			fNameEntry.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		setIfDefined(fNameEntry, site.getDescription() != null ? site.getDescription().getName() : null);
		setIfDefined(fURLEntry, site.getDescription() != null ? site.getDescription().getURL() : null);
		setIfDefined(fDescEntry, site.getDescription() != null ? site.getDescription().getText() : null);
		super.refresh();
	}

	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fURLEntry.cancelEdit();
		fDescEntry.cancelEdit();
		super.cancelEdit();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#canPaste(org.eclipse.swt.dnd.Clipboard)
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
