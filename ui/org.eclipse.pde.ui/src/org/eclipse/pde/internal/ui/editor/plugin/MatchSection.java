/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.Iterator;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class MatchSection extends PDESection implements IPartSelectionListener {
	private FormEntry versionText;
	private Button reexportButton;
	private Button optionalButton;
	private Label matchLabel;
	private ComboPart matchCombo;
	protected IPluginReference currentImport;
	protected IStructuredSelection multiSelection;
	private boolean blockChanges = false;
	private boolean addReexport = true;
	private boolean osgiMode = false;
	public static final String KEY_OPTIONAL = "ManifestEditor.MatchSection.optional"; //$NON-NLS-1$
	public static final String KEY_REEXPORT = "ManifestEditor.MatchSection.reexport"; //$NON-NLS-1$
	public static final String KEY_VERSION = "ManifestEditor.MatchSection.version"; //$NON-NLS-1$
	public static final String KEY_RULE = "ManifestEditor.MatchSection.rule"; //$NON-NLS-1$
	public static final String KEY_NONE = "ManifestEditor.MatchSection.none"; //$NON-NLS-1$
	public static final String KEY_PERFECT = "ManifestEditor.MatchSection.perfect"; //$NON-NLS-1$
	public static final String KEY_EQUIVALENT = "ManifestEditor.MatchSection.equivalent"; //$NON-NLS-1$
	public static final String KEY_COMPATIBLE = "ManifestEditor.MatchSection.compatible"; //$NON-NLS-1$
	public static final String KEY_GREATER = "ManifestEditor.MatchSection.greater"; //$NON-NLS-1$
	public static final String KEY_VERSION_FORMAT = "ManifestEditor.PluginSpecSection.versionFormat"; //$NON-NLS-1$
	public static final String KEY_VERSION_TITLE = "ManifestEditor.PluginSpecSection.versionTitle"; //$NON-NLS-1$
	/**
	 * @param formPage
	 * @param parent
	 * @param addReexport
	 */
	public MatchSection(PDEFormPage formPage, Composite parent,
			boolean addReexport) {
		super(formPage, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString("MatchSection.title")); //$NON-NLS-1$
		getSection().setDescription(
				PDEPlugin.getResourceString("MatchSection.desc")); //$NON-NLS-1$
		this.addReexport = addReexport;
		createClient(getSection(), formPage.getEditor().getToolkit());
	}
	public MatchSection(PDEFormPage formPage, Composite parent) {
		this(formPage, parent, true);
	}
	public void commit(boolean onSave) {
		if (isDirty() == false)
			return;
		if ((currentImport != null || multiSelection != null)
				&& versionText.getText().isEnabled()) {
			versionText.commit();
			String value = versionText.getValue();
			int match = IMatchRules.NONE;
			if (value != null && value.length() > 0) {
				applyVersion(value);
				match = getMatch();
			}
			applyMatch(match);
		}
		super.commit(onSave);
	}
	public void cancelEdit() {
		versionText.cancelEdit();
		super.cancelEdit();
	}
	public void createClient(Section section, FormToolkit toolkit) {
		//toolkit.createCompositeSeparator(section);
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.marginWidth = layout.marginHeight = 2;
		container.setLayout(layout);
		if (addReexport) {
			createOptionalButton(toolkit, container);
			createReexportButton(toolkit, container);
		}
		versionText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(KEY_VERSION), null, false);
		versionText.setFormEntryListener(new FormEntryAdapter(this, getPage()
				.getEditor().getEditorSite().getActionBars()) {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					if (value != null && value.length() > 0) {
						if (!isOsgiMode()) {
							PluginVersionIdentifier pvi = new PluginVersionIdentifier(
									text.getValue());
							String formatted = pvi.toString();
							text.setValue(formatted, true);
							applyVersion(formatted);
						} else {
							applyVersion(value);
						}
					} else {
						applyVersion(null);
					}
				} catch (RuntimeException e) {
					text.setValue(currentImport.getVersion(), true);
					String message = PDEPlugin
							.getResourceString(KEY_VERSION_FORMAT);
					MessageDialog.openError(
							PDEPlugin.getActiveWorkbenchShell(), PDEPlugin
									.getResourceString(KEY_VERSION_TITLE),
							message);
				}
			}
			public void textDirty(FormEntry text) {
				if (blockChanges)
					return;
				markDirty();
				blockChanges = true;
				if (!isOsgiMode())
					resetMatchCombo(currentImport);
				blockChanges = false;
			}
		});
		matchLabel = toolkit.createLabel(container, PDEPlugin
				.getResourceString(KEY_RULE));
		matchLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		matchCombo = new ComboPart();
		matchCombo.createControl(container, toolkit, SWT.READ_ONLY);
		matchCombo.add(PDEPlugin.getResourceString(KEY_NONE));
		matchCombo.add(PDEPlugin.getResourceString(KEY_EQUIVALENT));
		matchCombo.add(PDEPlugin.getResourceString(KEY_COMPATIBLE));
		matchCombo.add(PDEPlugin.getResourceString(KEY_PERFECT));
		matchCombo.add(PDEPlugin.getResourceString(KEY_GREATER));
		//matchCombo.pack();
		matchCombo.getControl().setLayoutData(
				new GridData(GridData.FILL_HORIZONTAL));
		matchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!blockChanges) {
					applyMatch(matchCombo.getSelectionIndex());
				}
			}
		});
		toolkit.paintBordersFor(container);
		initialize();
		update((IPluginReference) null);
		section.setClient(container);
	}
	private void createReexportButton(FormToolkit toolkit, Composite container) {
		reexportButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_REEXPORT), SWT.CHECK);
		reexportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockChanges)
					return;
				if (!(currentImport instanceof IPluginImport))
					return;
				if (currentImport != null) {
					try {
						IPluginImport iimport = (IPluginImport) currentImport;
						iimport.setReexported(reexportButton.getSelection());
					} catch (CoreException ex) {
						PDEPlugin.logException(ex);
					}
				}
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		reexportButton.setLayoutData(gd);
	}
	private void createOptionalButton(FormToolkit toolkit, Composite container) {
		optionalButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_OPTIONAL), SWT.CHECK);
		optionalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockChanges)
					return;
				if (!(currentImport instanceof IPluginImport))
					return;
				if (currentImport != null) {
					try {
						IPluginImport iimport = (IPluginImport) currentImport;
						//ignoreModelEvents = true;
						iimport.setOptional(optionalButton.getSelection());
						//ignoreModelEvents = false;
					} catch (CoreException ex) {
						PDEPlugin.logException(ex);
					}
				}
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		optionalButton.setLayoutData(gd);
	}
	private void applyVersion(String version) {
		try {
			if (currentImport != null) {
				currentImport.setVersion(version);
			} else if (multiSelection != null) {
				for (Iterator iter = multiSelection.iterator(); iter.hasNext();) {
					IPluginReference reference = (IPluginReference) iter.next();
					reference.setVersion(version);
				}
			}
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}
	private void applyMatch(int match) {
		try {
			if (currentImport != null) {
				currentImport.setMatch(match);
			} else if (multiSelection != null) {
				for (Iterator iter = multiSelection.iterator(); iter.hasNext();) {
					IPluginReference reference = (IPluginReference) iter.next();
					reference.setMatch(match);
				}
			}
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}
	private int getMatch() {
		return matchCombo.getSelectionIndex();
	}
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
		updateMode();
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(currentImport)) {
				update((IPluginReference) null);
			}
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = e.getChangedObjects()[0];
			if (object.equals(currentImport)) {
				update(currentImport);
			}
		}
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size()==1) {
			Object changeObject = ((IStructuredSelection) selection)
			.getFirstElement();
			IPluginReference input = null;			
			if (changeObject instanceof ImportObject)
				input = ((ImportObject) changeObject).getImport();
			else if (changeObject instanceof IPluginReference)
				input = (IPluginReference) changeObject;
			update(input);
		}
		else update(ssel);
	}
	private void resetMatchCombo(IPluginReference iimport) {
		String text = versionText.getText().getText();
		boolean enable = isEditable() && text.length() > 0;
		matchCombo.getControl().setEnabled(enable);
		setMatchCombo(iimport);
	}
	private void setMatchCombo(IPluginReference iimport) {
		int match = iimport != null ? iimport.getMatch() : IMatchRules.NONE;
		matchCombo.select(match);
	}
	protected void update(IStructuredSelection selection) {
		blockChanges = true;
		currentImport = null;
		int size = selection.size();
		if (size == 0) {
			versionText.setValue(null, true);
			boolean enableState = false;
			versionText.getText().setEditable(enableState);
			matchCombo.getControl().setEnabled(enableState);
			matchCombo.setText(""); //$NON-NLS-1$
			blockChanges = false;
			return;
		}
		if (multiSelection != null && !multiSelection.equals(selection)
				&& isEditable()) {
			commit(false);
		}
		multiSelection = selection;
		versionText.getText().setEditable(isEditable());
		if (size == 1) {
			IPluginReference ref = (IPluginReference) selection
					.getFirstElement();
			versionText.setValue(ref.getVersion());
			resetMatchCombo(ref);
		} else {
			versionText.setValue(""); //$NON-NLS-1$
			matchCombo.getControl().setEnabled(true);
			setMatchCombo(null);
		}
		blockChanges = false;
	}
	protected void update(IPluginReference iimport) {
		blockChanges = true;
		if (iimport == null) {
			if (addReexport) {
				optionalButton.setSelection(false);
				optionalButton.setEnabled(false);
				reexportButton.setSelection(false);
				reexportButton.setEnabled(false);
			}
			versionText.setValue(null, true);
			boolean enableState = false;
			versionText.getText().setEditable(enableState);
			matchCombo.getControl().setEnabled(enableState);
			matchCombo.setText(""); //$NON-NLS-1$
			currentImport = null;
			blockChanges = false;
			return;
		}
		if (currentImport != null && !iimport.equals(currentImport)
				&& isEditable()) {
			commit(false);
		}
		currentImport = iimport;
		if (currentImport instanceof IPluginImport) {
			IPluginImport pimport = (IPluginImport) currentImport;
			optionalButton.setEnabled(isEditable());
			optionalButton.setSelection(pimport.isOptional());
			reexportButton.setEnabled(isEditable());
			reexportButton.setSelection(pimport.isReexported());
		}
		versionText.getText().setEditable(isEditable());
		versionText.setValue(currentImport.getVersion());
		resetMatchCombo(currentImport);
		blockChanges = false;
	}
	/**
	 * @return Returns the osgiMode.
	 */
	public boolean isOsgiMode() {
		return osgiMode;
	}
	/**
	 * @param osgiMode
	 *            The osgiMode to set.
	 */
	public void setOsgiMode(boolean osgiMode) {
		this.osgiMode = osgiMode;
		updateMode();
	}
	private void updateMode() {
		// hide the match combo
		matchLabel.setVisible(!isOsgiMode());
		matchCombo.getControl().setVisible(!isOsgiMode());
	}
}
