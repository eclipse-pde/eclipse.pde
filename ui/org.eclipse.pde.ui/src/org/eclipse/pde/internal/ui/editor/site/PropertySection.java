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
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class PropertySection extends PDEFormSection {
	public static final String KEY_URL = "SiteEditor.PropertySection.url";
	public static final String KEY_ID = "SiteEditor.PropertySection.id";
	private static final String KEY_VERSION =
		"SiteEditor.PropertySection.version";
	private static final String KEY_TYPE = "SiteEditor.PropertySection.type";
	private static final String KEY_PATH = "SiteEditor.PropertySection.path";

	private Class objClass;
	private FormEntry[] entries;
	private Object currentInput;

	private boolean updateNeeded;

	public PropertySection(
		PDEFormPage page,
		String title,
		String desc,
		Class objClass) {
		super(page);
		setHeaderText(title);
		setDescription(desc);
		this.objClass = objClass;
	}

	public void commitChanges(boolean onSave) {
		for (int i = 0; i < entries.length; i++) {
			entries[i].commit();
		}
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

		if (objClass.equals(ISiteFeature.class)) {
			entries = new FormEntry[4];
			int index = 0;
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_URL),
				factory,
				index++);
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_ID),
				factory,
				index++);
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_VERSION),
				factory,
				index++);
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_TYPE),
				factory,
				index++);
		} else if (objClass.equals(ISiteArchive.class)) {
			entries = new FormEntry[2];
			int index = 0;
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_PATH),
				factory,
				index++);
			createEntry(
				container,
				PDEPlugin.getResourceString(KEY_URL),
				factory,
				index++);
		}
		GridData gd = (GridData) entries[0].getControl().getLayoutData();
		gd.widthHint = 100;
		factory.paintBordersFor(container);
		return container;
	}

	private void createEntry(
		Composite container,
		String name,
		FormWidgetFactory factory,
		final int index) {
		FormEntry entry =
			new FormEntry(createText(container, name, factory, 1));
		entry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setEntryText(index, text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		entries[index] = entry;
	}

	private void setEntryText(int index, String text) {
		try {
			if (objClass.equals(ISiteFeature.class)) {
				ISiteFeature feature = (ISiteFeature) currentInput;
				switch (index) {
					case 0 :
						feature.setURL(text);
						break;
					case 1 :
						feature.setId(text);
						break;
					case 2 :
						feature.setVersion(text);
						break;
					case 3 :
						feature.setType(text);
						break;
				}
			} else if (objClass.equals(ISiteArchive.class)) {
				ISiteArchive archive = (ISiteArchive) currentInput;
				switch (index) {
					case 0 :
						archive.setPath(text);
						break;
					case 1 :
						archive.setURL(text);
						break;
				}
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
			for (int i = 0; i < entries.length; i++) {
				entries[i].getControl().setEditable(false);
			}
		}
		model.addModelChangedListener(this);
	}

	public boolean isDirty() {
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isDirty())
				return true;
		}
		return false;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length > 0
				&& objClass.isAssignableFrom(objs[0].getClass())) {
				updateNeeded = true;
				if (getFormPage().isVisible())
					update();
			}
		}
	}
	public void setFocus() {
		if (entries != null)
			entries[0].getControl().setFocus();
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
		//ISiteModel model = (ISiteModel) input;
		//ISite site = model.getSite();

		inputChanged(currentInput);
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		if (changeType == SELECTION) {
			inputChanged(changeObject);
		}
	}

	private void inputChanged(Object changeObject) {
		currentInput = changeObject;

		if (currentInput == null) {
			clearEntries();
			return;
		}
		setEntriesEditable(true);
		if (objClass.isAssignableFrom(ISiteFeature.class)) {
			ISiteFeature feature = (ISiteFeature) changeObject;
			entries[0].setValue(feature.getURL(), true);
			entries[1].setValue(feature.getId(), true);
			entries[2].setValue(feature.getVersion(), true);
			entries[3].setValue(feature.getType(), true);
		} else if (objClass.isAssignableFrom(ISiteArchive.class)) {
			ISiteArchive archive = (ISiteArchive) changeObject;
			entries[0].setValue(archive.getPath(), true);
			entries[1].setValue(archive.getURL(), true);
		}
	}

	private void clearEntries() {
		for (int i = 0; i < entries.length; i++) {
			FormEntry entry = entries[i];
			entry.setValue(null, true);
			entry.getControl().setEditable(false);
		}
	}

	private void setEntriesEditable(boolean value) {
		for (int i = 0; i < entries.length; i++) {
			FormEntry entry = entries[i];
			entry.getControl().setEditable(value);
		}
	}
	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}

}
