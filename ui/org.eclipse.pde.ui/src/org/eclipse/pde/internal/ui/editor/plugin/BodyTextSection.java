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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class BodyTextSection
	extends PDESection
	implements IModelChangedListener, IPartSelectionListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.BodyTextSection.title"; //$NON-NLS-1$
	public static final String SECTION_TITLE_FULL =
		"ManifestEditor.BodyTextSection.titleFull"; //$NON-NLS-1$
	public static final String KEY_APPLY = "Actions.apply.flabel"; //$NON-NLS-1$
	public static final String KEY_RESET = "Actions.reset.flabel"; //$NON-NLS-1$
	public static final String KEY_DELETE = "Actions.delete.flabel"; //$NON-NLS-1$
	private Button applyButton;
	private Button resetButton;
	private IPluginElement currentElement;
	private Text text;
	private boolean blockNotification;

	public BodyTextSection(ExtensionsPage page, Composite parent) {
		super(page, parent, Section.TWISTIE);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void updateTitle(boolean hasContents) {
		String title;
		if (hasContents)
			title = PDEPlugin.getResourceString(SECTION_TITLE_FULL);
		else
			title = PDEPlugin.getResourceString(SECTION_TITLE);
		if (!getSection().getText().equals(title)) {
			getSection().setText(title);
			getSection().layout();
		}
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		GridData gd;

		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		container.setLayout(layout);

		// text
		text =
			toolkit.createText(
				container,
				"", //$NON-NLS-1$
				SWT.MULTI
					| SWT.WRAP
					| SWT.V_SCROLL);
		text.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH);
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (blockNotification) return;
				markDirty();
				applyButton.setEnabled(true);
				resetButton.setEnabled(true);
			}
		});
		text.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				getPage().getPDEEditor().getContributor().updateSelectableActions(new StructuredSelection());
			}
		});

		Composite buttonContainer = toolkit.createComposite(container);
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
			toolkit.createButton(
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
			toolkit.createButton(
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

		if (SWT.getPlatform().equals("motif") == false) //$NON-NLS-1$
			toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model!=null)
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
			if (currentElement!=null)
				currentElement.setText(
						text.getText().length() > 0 ? text.getText() : ""); //$NON-NLS-1$
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		applyButton.setEnabled(false);
	}

	public void commit(boolean onSave) {
		handleApply();
		if (onSave) {
			resetButton.setEnabled(false);
		}
		super.commit(onSave);
	}

	private void handleReset() {
		updateText(currentElement);
		resetButton.setEnabled(false);
		applyButton.setEnabled(false);
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		text.setEditable(model.isEditable());
		updateInput();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			return;
		}
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		Object changeObject = ((IStructuredSelection)selection).getFirstElement();
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
		text.setEditable(isEditable() && currentElement != null);
	}

	private void updateText(IPluginElement element) {
		String bodyText = element != null ? element.getText() : null;

		blockNotification=true;
		text.setText(bodyText != null && bodyText.length()>0? bodyText : ""); //$NON-NLS-1$
		applyButton.setEnabled(false);
		resetButton.setEnabled(false);

		updateTitle(bodyText != null && bodyText.length()>0);
		blockNotification=false;
	}
	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
}
