/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class BodyTextSection extends PDESection implements IModelChangedListener, IPartSelectionListener {
	private Button fApplyButton;
	private Button fResetButton;
	private IPluginElement fCurrentElement;
	private Text fText;
	private boolean fBlockNotification;

	public BodyTextSection(ExtensionsPage page, Composite parent) {
		super(page, parent, ExpandableComposite.TWISTIE);
		getSection().setText(PDEUIMessages.ManifestEditor_BodyTextSection_title);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void updateTitle(boolean hasContents) {
		String title;
		if (hasContents)
			title = PDEUIMessages.ManifestEditor_BodyTextSection_titleFull;
		else
			title = PDEUIMessages.ManifestEditor_BodyTextSection_title;
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
		fText = toolkit.createText(container, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		fText.setEditable(false);
		fText.setLayoutData(new GridData(GridData.FILL_BOTH));
		fText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fBlockNotification) return;
				markDirty();
				fApplyButton.setEnabled(true);
				fResetButton.setEnabled(true);
			}
		});
		fText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				getPage().getPDEEditor().getContributor().updateSelectableActions(new StructuredSelection());
			}
		});

		Composite buttonContainer = toolkit.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);

		// add buttons
		fApplyButton = toolkit.createButton(buttonContainer,	PDEUIMessages.Actions_apply_flabel,	SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fApplyButton.setLayoutData(gd);
		fApplyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleApply();
			}
		});

		fResetButton = toolkit.createButton(buttonContainer, PDEUIMessages.Actions_reset_flabel, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fResetButton.setLayoutData(gd);
		fResetButton.addSelectionListener(new SelectionAdapter() {
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
			fText.cut();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			fText.cut();
			return false;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fText.selectAll();
			return true;
		}
		if (actionId.equals(ActionFactory.COPY.getId())) {
			fText.copy();
			return true;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			fText.paste();
			return true;
		}
		return false;
	}

	private void handleApply() {
		try {
			if (fCurrentElement != null)
				fCurrentElement.setText(fText.getText().length() > 0 ? fText.getText() : ""); //$NON-NLS-1$
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		fApplyButton.setEnabled(false);
		fResetButton.setEnabled(false);
	}

	public void commit(boolean onSave) {
		handleApply();
		if (onSave) {
			fResetButton.setEnabled(false);
		}
		super.commit(onSave);
	}

	private void handleReset() {
		updateText(fCurrentElement);
		fResetButton.setEnabled(false);
		fApplyButton.setEnabled(false);
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		fText.setEditable(model.isEditable());
		updateInput();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			return;
		}
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		Object changeObject = ((IStructuredSelection)selection).getFirstElement();
		if (fCurrentElement != null) {
			if (fCurrentElement == changeObject)
				return;
			if (fApplyButton.isEnabled() && 
					MessageDialog.openQuestion(
							getSection().getShell(),
							PDEUIMessages.BodyTextSection_saveBodyText,
							NLS.bind(PDEUIMessages.BodyTextSection_saveBodyTextMessage, fCurrentElement.getName())))
				handleApply();
		}
		if (changeObject instanceof IPluginElement)
			fCurrentElement = (IPluginElement) changeObject;
		else
			fCurrentElement = null;
		updateInput();
	}
	private void updateInput() {
		fApplyButton.setEnabled(false);
		fResetButton.setEnabled(false);
		updateText(fCurrentElement);
		fText.setEditable(isEditable() && fCurrentElement != null);
	}

	private void updateText(IPluginElement element) {
		String bodyText = element != null ? element.getText() : null;

		fBlockNotification=true;
		fText.setText(bodyText != null && bodyText.length()>0? bodyText : ""); //$NON-NLS-1$
		fApplyButton.setEnabled(false);
		fResetButton.setEnabled(false);

		updateTitle(bodyText != null && bodyText.length()>0);
		fBlockNotification=false;
	}
	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
}
