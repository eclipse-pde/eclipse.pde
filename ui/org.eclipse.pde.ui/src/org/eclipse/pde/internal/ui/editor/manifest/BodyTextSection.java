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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.update.ui.forms.internal.*;

public class BodyTextSection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.BodyTextSection.title";
	public static final String SECTION_TITLE_FULL =
		"ManifestEditor.BodyTextSection.titleFull";
	public static final String KEY_APPLY = "Actions.apply.flabel";
	public static final String KEY_RESET = "Actions.reset.flabel";
	public static final String KEY_DELETE = "Actions.delete.flabel";
	private Button applyButton;
	private Button resetButton;
	private IPluginElement currentElement;
	private Text text;

	public BodyTextSection(ManifestExtensionsPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	}

	private void updateTitle(boolean hasContents) {
		String title;
		if (hasContents)
			title = PDEPlugin.getResourceString(SECTION_TITLE_FULL);
		else
			title = PDEPlugin.getResourceString(SECTION_TITLE);
		setHeaderText(title);
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		GridData gd;

		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		container.setLayout(layout);

		// text
		text =
			factory.createText(
				container,
				"",
				SWT.MULTI
					| SWT.WRAP
					| SWT.V_SCROLL
					| FormWidgetFactory.BORDER_STYLE);
		text.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH);
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				applyButton.setEnabled(true);
				resetButton.setEnabled(true);
			}
		});

		Composite buttonContainer = factory.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING
					| GridData.VERTICAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);

		// add buttons
		applyButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_APPLY),
				SWT.PUSH);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		applyButton.setLayoutData(gd);
		applyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleApply();
			}
		});

		resetButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_RESET),
				SWT.PUSH);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		resetButton.setLayoutData(gd);
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReset();
			}
		});

		if (SWT.getPlatform().equals("motif") == false)
			factory.paintBordersFor(container);
		return container;
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			text.selectAll();
			return true;
		}
		if (actionId.equals(ActionFactory.COPY.getId())) {
			text.copy();
			return true;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			text.paste();
			return true;
		}
		return false;
	}

	private void handleDelete() {
		text.cut();
	}

	private void handleApply() {
		try {
			currentElement.setText(
				text.getText().length() > 0 ? text.getText() : null);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		applyButton.setEnabled(false);
	}

	private void handleReset() {
		updateText(currentElement);
		resetButton.setEnabled(false);
		applyButton.setEnabled(false);
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		model.addModelChangedListener(this);
		setReadOnly(!model.isEditable());
		text.setEditable(model.isEditable());
		updateInput();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			return;
		}
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		if (currentElement != null && currentElement == changeObject)
			return;
		if (changeObject instanceof IPluginElement)
			this.currentElement = (IPluginElement) changeObject;
		else
			currentElement = null;
		updateInput();
	}
	private void updateInput() {
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);
		updateText(currentElement);
		text.setEditable(!isReadOnly() && currentElement != null);
	}

	private void updateText(IPluginElement element) {
		String bodyText = element != null ? element.getText() : null;

		text.setText(bodyText != null ? bodyText : "");
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);

		updateTitle(bodyText != null);
	}
}
