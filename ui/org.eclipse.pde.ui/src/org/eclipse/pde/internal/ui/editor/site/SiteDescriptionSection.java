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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class SiteDescriptionSection extends PDEFormSection {
	public static final String SECTION_TITLE =
		"SiteEditor.SiteDescriptionSection.title";
	public static final String SECTION_DESC =
		"SiteEditor.SiteDescriptionSection.desc";
	public static final String SECTION_URL =
		"SiteEditor.SiteDescriptionSection.url";
	public static final String SECTION_TEXT =
		"SiteEditor.SiteDescriptionSection.text";

	private FormEntry url;
	private FormEntry text;

	private boolean updateNeeded;

	public SiteDescriptionSection(SitePage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public void commitChanges(boolean onSave) {
		url.commit();
		text.commit();
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		url =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_URL),
					factory,
					1));
		url.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionURL(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		text =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(SECTION_TEXT),
					factory,
					1,
					FormWidgetFactory.BORDER_STYLE | SWT.WRAP | SWT.MULTI));
		text.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setDescriptionText(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		GridData gd = (GridData)text.getControl().getLayoutData();
		gd.heightHint = 150;
		gd.widthHint = 100;
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

		if (model.isEditable() == false) {
			url.getControl().setEditable(false);
			text.getControl().setEditable(false);
		}
		model.addModelChangedListener(this);
	}

	public boolean isDirty() {
		return url.isDirty() || text.isDirty();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length > 0 && objs[0] instanceof ISite) {
				updateNeeded = true;
				if (getFormPage().isVisible())
					update();
			}
		}
	}
	public void setFocus() {
		if (url != null)
			url.getControl().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}
	private void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
	}
	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}
	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISite site = model.getSite();
		setIfDefined(
			url,
			site.getDescription() != null
				? site.getDescription().getURL()
				: null);
		setIfDefined(
			text,
			site.getDescription() != null
				? site.getDescription().getText()
				: null);
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}

}
