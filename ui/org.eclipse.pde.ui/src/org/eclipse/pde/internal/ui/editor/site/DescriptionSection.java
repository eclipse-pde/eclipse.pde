/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;

/**
 * 
 */
public class DescriptionSection extends PDESection {
	private FormEntry fURLEntry;
	private FormEntry fDescEntry;
	public DescriptionSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection()
				.setText(
						PDEPlugin
								.getResourceString("SiteEditor.DescriptionSection.header")); //$NON-NLS-1$
		getSection()
				.setDescription(
						PDEPlugin
								.getResourceString("SiteEditor.DescriptionSection.desc")); //$NON-NLS-1$
		createClient(getSection(), page.getManagedForm().getToolkit());		
	}
	public void commit(boolean onSave) {
		fURLEntry.commit();
		fDescEntry.commit();
		super.commit(onSave);
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		fURLEntry = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString("SiteEditor.DescriptionSection.urlLabel"), //$NON-NLS-1$
				null, false);
		fURLEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setDescriptionURL(text.getValue());
			}
		});
		fDescEntry = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString("SiteEditor.DescriptionSection.descLabel"), //$NON-NLS-1$
				SWT.WRAP | SWT.MULTI);
		fDescEntry.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fDescEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				setDescriptionText(text.getValue());
			}
		});
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
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
		if (model!=null)
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
		if (fURLEntry != null)
			fURLEntry.getText().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}
	public void refresh() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		ISite site = model.getSite();
		setIfDefined(fURLEntry, site.getDescription() != null ? site
				.getDescription().getURL() : null);
		setIfDefined(fDescEntry, site.getDescription() != null ? site
				.getDescription().getText() : null);
		super.refresh();
	}
	public void cancelEdit() {
		fURLEntry.cancelEdit();
		fDescEntry.cancelEdit();
		super.cancelEdit();
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[]{TextTransfer.getInstance(),
				RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
}
