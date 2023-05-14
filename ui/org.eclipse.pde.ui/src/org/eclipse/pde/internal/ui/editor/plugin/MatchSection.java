/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class MatchSection extends PDESection implements IPartSelectionListener {

	private Button fReexportButton;
	private Button fOptionalButton;

	private FormEntry fVersionText;

	private ComboPart fMatchCombo;
	protected IPluginReference fCurrentImport;

	private boolean fBlockChanges = false;
	private boolean fAddReexport = true;

	public MatchSection(PDEFormPage formPage, Composite parent, boolean addReexport) {
		super(formPage, parent, Section.DESCRIPTION);
		fAddReexport = addReexport;
		createClient(getSection(), formPage.getEditor().getToolkit());
	}

	@Override
	public void commit(boolean onSave) {
		if (isDirty() == false)
			return;
		if (fCurrentImport != null && fVersionText.getText().isEnabled()) {
			fVersionText.commit();
			String value = fVersionText.getValue();
			int match = IMatchRules.NONE;
			if (value != null && value.length() > 0) {
				applyVersion(value);
				match = getMatch();
			}
			applyMatch(match);
		}
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fVersionText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		if (fAddReexport) {
			createOptionalButton(toolkit, container);
			createReexportButton(toolkit, container);
		}

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.ManifestEditor_MatchSection_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this, getPage().getEditor().getEditorSite().getActionBars()) {
			@Override
			public void textValueChanged(FormEntry text) {
				applyVersion(text.getValue());
			}

			@Override
			public void textDirty(FormEntry text) {
				if (fBlockChanges)
					return;
				markDirty();
				fBlockChanges = true;
				resetMatchCombo(fCurrentImport);
				fBlockChanges = false;
			}
		});

		Label matchLabel = toolkit.createLabel(container, PDEUIMessages.ManifestEditor_PluginSpecSection_versionMatch);
		matchLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fMatchCombo = new ComboPart();
		fMatchCombo.createControl(container, toolkit, SWT.READ_ONLY);
		fMatchCombo.add(""); //$NON-NLS-1$
		fMatchCombo.add(PDEUIMessages.ManifestEditor_MatchSection_equivalent);
		fMatchCombo.add(PDEUIMessages.ManifestEditor_MatchSection_compatible);
		fMatchCombo.add(PDEUIMessages.ManifestEditor_MatchSection_perfect);
		fMatchCombo.add(PDEUIMessages.ManifestEditor_MatchSection_greater);
		fMatchCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMatchCombo.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockChanges) {
				applyMatch(fMatchCombo.getSelectionIndex());
			}
		}));
		toolkit.paintBordersFor(container);
		initialize();
		update((IPluginReference) null);

		section.setClient(container);
		section.setText(PDEUIMessages.MatchSection_title);
		section.setDescription(PDEUIMessages.MatchSection_desc);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
	}

	private void createReexportButton(FormToolkit toolkit, Composite container) {
		fReexportButton = toolkit.createButton(container, PDEUIMessages.ManifestEditor_MatchSection_reexport, SWT.CHECK);
		fReexportButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockChanges && fCurrentImport instanceof IPluginImport) {
				try {
					IPluginImport iimport = (IPluginImport) fCurrentImport;
					iimport.setReexported(fReexportButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		}));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fReexportButton.setLayoutData(gd);
	}

	private void createOptionalButton(FormToolkit toolkit, Composite container) {
		fOptionalButton = toolkit.createButton(container, PDEUIMessages.ManifestEditor_MatchSection_optional, SWT.CHECK);
		fOptionalButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (fBlockChanges)
				return;
			if (!fBlockChanges && fCurrentImport instanceof IPluginImport) {
				try {
					IPluginImport iimport = (IPluginImport) fCurrentImport;
					iimport.setOptional(fOptionalButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		}));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fOptionalButton.setLayoutData(gd);
	}

	private void applyVersion(String version) {
		try {
			if (fCurrentImport != null) {
				fCurrentImport.setVersion(version);
			}
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}

	private void applyMatch(int match) {
		try {
			if (fCurrentImport != null) {
				fCurrentImport.setMatch(match);
			}
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}

	private int getMatch() {
		return fMatchCombo.getSelectionIndex();
	}

	@Override
	public void dispose() {
		IModel model = (IModel) getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}

	private void initialize() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(fCurrentImport)) {
				update((IPluginReference) null);
			}
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = e.getChangedObjects()[0];
			if (object.equals(fCurrentImport)) {
				update(fCurrentImport);
			}
		}
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			Object changeObject = ((IStructuredSelection) selection).getFirstElement();
			IPluginReference input = null;
			if (changeObject instanceof ImportObject)
				input = ((ImportObject) changeObject).getImport();
			else if (changeObject instanceof IPluginReference)
				input = (IPluginReference) changeObject;
			update(input);
		} else {
			update(null);
		}
	}

	private void resetMatchCombo(IPluginReference iimport) {
		fMatchCombo.getControl().setEnabled(isEditable() && fVersionText.getText().getText().length() > 0);
		setMatchCombo(iimport);
	}

	private void setMatchCombo(IPluginReference iimport) {
		fMatchCombo.select(iimport != null ? iimport.getMatch() : IMatchRules.NONE);
	}

	protected void update(IPluginReference iimport) {
		fBlockChanges = true;
		if (iimport == null) {
			if (fAddReexport) {
				fOptionalButton.setSelection(false);
				fOptionalButton.setEnabled(false);
				fReexportButton.setSelection(false);
				fReexportButton.setEnabled(false);
			}
			fVersionText.setValue(null, true);
			fVersionText.setEditable(false);
			fMatchCombo.getControl().setEnabled(false);
			fMatchCombo.setText(""); //$NON-NLS-1$
			fCurrentImport = null;
			fBlockChanges = false;
			return;
		}

		if (fCurrentImport != null && !iimport.equals(fCurrentImport) && isEditable()) {
			commit(false);
		}

		fCurrentImport = iimport;
		if (fCurrentImport instanceof IPluginImport) {
			IPluginImport pimport = (IPluginImport) fCurrentImport;
			fOptionalButton.setEnabled(isEditable());
			fOptionalButton.setSelection(pimport.isOptional());
			fReexportButton.setEnabled(isEditable());
			fReexportButton.setSelection(pimport.isReexported());
		}
		fVersionText.setEditable(isEditable());
		fVersionText.setValue(fCurrentImport.getVersion());
		resetMatchCombo(fCurrentImport);
		fBlockChanges = false;
	}
}
