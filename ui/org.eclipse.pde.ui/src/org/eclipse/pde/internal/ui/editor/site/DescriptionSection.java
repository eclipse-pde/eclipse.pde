/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class DescriptionSection extends PDEFormSection {
	private FormEntry fURLEntry;
	private FormEntry fDescEntry;
	private boolean fUpdateNeeded;


	public DescriptionSection(PDEFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.header")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.desc")); //$NON-NLS-1$
	}
	
	public void commitChanges(boolean onSave) {
		fURLEntry.commit();
		fDescEntry.commit();
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
				
		fURLEntry =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString("SiteEditor.DescriptionSection.urlLabel"), //$NON-NLS-1$
					factory,
					1));
		fURLEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionURL(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		fURLEntry.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Text area =
			createText(
				container,
				PDEPlugin.getResourceString("SiteEditor.DescriptionSection.descLabel"), //$NON-NLS-1$
				factory,
				1,
				FormWidgetFactory.BORDER_STYLE | SWT.WRAP | SWT.MULTI);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fDescEntry = new FormEntry(area);
		fDescEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionText(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		factory.paintBordersFor(container);
		return container;
	}
	
	private void setDescriptionURL(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
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
		ISiteModel model = (ISiteModel) getFormPage().getModel();
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

	private void forceDirty() {
		setDirty(true);
		ISiteModel model = (ISiteModel) getFormPage().getModel();

		if (model instanceof IEditable) {
			((IEditable) model).setDirty(true);
		}
		getFormPage().getEditor().fireSaveNeeded();
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize(Object input) {
		ISiteModel model = (ISiteModel) input;
		update(input);
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		fUpdateNeeded = true;
		update();
	}
	
	public void setFocus() {
		if (fURLEntry != null)
			fURLEntry.getControl().setFocus();
	}
	
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}
	
	public void update() {
		if (fUpdateNeeded) {
			update(getFormPage().getModel());			
		}
	}
	
	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISite site = model.getSite();
		setIfDefined(
			fURLEntry,
			site.getDescription() != null
				? site.getDescription().getURL()
				: null);
		setIfDefined(
			fDescEntry,
			site.getDescription() != null
				? site.getDescription().getText()
				: null);
		fUpdateNeeded = false;
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers =
			new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

}
