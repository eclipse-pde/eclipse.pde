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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class MatchSection extends PDESection implements IPartSelectionListener {
	private FormEntry versionText;
	private Button reexportButton;
	private Button optionalButton;
	private ComboPart matchCombo;
	protected IPluginReference currentImport;
	protected IStructuredSelection multiSelection;
	private boolean blockChanges = false;
	private boolean ignoreModelEvents = false;
	private boolean addReexport = true;
	public static final String SECTION_TITLE = "ManifestEditor.MatchSection.title";
	public static final String SECTION_DESC = "ManifestEditor.MatchSection.desc";
	public static final String KEY_OPTIONAL = "ManifestEditor.MatchSection.optional";
	public static final String KEY_REEXPORT = "ManifestEditor.MatchSection.reexport";
	public static final String KEY_VERSION = "ManifestEditor.MatchSection.version";
	public static final String KEY_RULE = "ManifestEditor.MatchSection.rule";
	public static final String KEY_NONE = "ManifestEditor.MatchSection.none";
	public static final String KEY_PERFECT = "ManifestEditor.MatchSection.perfect";
	public static final String KEY_EQUIVALENT = "ManifestEditor.MatchSection.equivalent";
	public static final String KEY_COMPATIBLE = "ManifestEditor.MatchSection.compatible";
	public static final String KEY_GREATER = "ManifestEditor.MatchSection.greater";
	public static final String KEY_VERSION_FORMAT = "ManifestEditor.PluginSpecSection.versionFormat";
	public static final String KEY_VERSION_TITLE = "ManifestEditor.PluginSpecSection.versionTitle";
	/**
	 * @param formPage
	 * @param parent
	 * @param addReexport
	 */
	public MatchSection(PDEFormPage formPage, Composite parent,
			boolean addReexport) {
		super(formPage, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		this.addReexport = addReexport;
		createClient(getSection(), formPage.getEditor().getToolkit());
	}
	public MatchSection(PDEFormPage formPage, Composite parent) {
		this(formPage, parent, true);
	}
	public void commit(boolean onSave) {
		/*
		 * if (isDirty() == false) return; ignoreModelEvents = true; if
		 * ((currentImport != null || multiSelection != null) &&
		 * versionText.getControl().isEnabled()) { versionText.commit(); String
		 * value = versionText.getValue(); int match = IPluginImport.NONE; if
		 * (value != null && value.length() > 0) { applyVersion(value); match =
		 * getMatch(); } applyMatch(match); } setDirty(false);
		 * ignoreModelEvents = false;
		 */
		super.commit(onSave);
	}
	public void createClient(Section section, FormToolkit toolkit) {
		toolkit.createCompositeSeparator(section);
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
		versionText.setFormEntryListener(new FormEntryAdapter(this,
				getPage().getEditor().getEditorSite().getActionBars()) {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					ignoreModelEvents = true;
					if (value != null && value.length() > 0) {
						PluginVersionIdentifier pvi = new PluginVersionIdentifier(
								text.getValue());
						String formatted = pvi.toString();
						text.setValue(formatted, true);
						applyVersion(formatted);
					} else {
						applyVersion(null);
					}
					ignoreModelEvents = false;
				} catch (Throwable e) {
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
				forceDirty();
				blockChanges = true;
				resetMatchCombo(currentImport);
				blockChanges = false;
			}
		});
		toolkit.createLabel(container, PDEPlugin.getResourceString(KEY_RULE));
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
						ignoreModelEvents = true;
						iimport.setReexported(reexportButton.getSelection());
						ignoreModelEvents = false;
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
	private void forceDirty() {
		/*
		 * setDirty(true); IModel model = (IModel) getFormPage().getModel(); if
		 * (model instanceof IEditable) { IEditable editable = (IEditable)
		 * model; editable.setDirty(true);
		 * getFormPage().getEditor().fireSaveNeeded(); }
		 */
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
		/*
		 * IModel model = (IModel) getFormPage().getModel(); if (model
		 * instanceof IModelChangeProvider) ((IModelChangeProvider)
		 * model).removeModelChangedListener(this);
		 */
		super.dispose();
	}
	public void initialize(Object input) {
		/*
		 * IModel model = (IModel) input; setReadOnly(!model.isEditable()); if
		 * (model instanceof IModelChangeProvider) ((IModelChangeProvider)
		 * model).addModelChangedListener(this);
		 */
	}
	public void modelChanged(IModelChangedEvent e) {
		if (ignoreModelEvents)
			return;
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
		IPluginReference input = null;
		Object changeObject = ((IStructuredSelection) selection)
				.getFirstElement();
		if (changeObject instanceof ImportObject)
			input = ((ImportObject) changeObject).getImport();
		else if (changeObject instanceof IPluginReference)
			input = (IPluginReference) changeObject;
		else if (changeObject instanceof IStructuredSelection) {
			update((IStructuredSelection) changeObject);
			return;
		}
		update(input);
	}
	private void resetMatchCombo(IPluginReference iimport) {
		String text = versionText.getText().getText();
		//boolean enable = !isReadOnly() && text.length() > 0;
		boolean enable = true;
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
			matchCombo.setText("");
			blockChanges = false;
			return;
		}
		/*
		 * if (multiSelection != null && !multiSelection.equals(selection) &&
		 * !isReadOnly()) { commitChanges(false); }
		 */
		multiSelection = selection;
		//versionText.getControl().setEditable(!isReadOnly());
		if (size == 1) {
			IPluginReference ref = (IPluginReference) selection
					.getFirstElement();
			versionText.setValue(ref.getVersion());
			resetMatchCombo(ref);
		} else {
			versionText.setValue("");
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
			matchCombo.setText("");
			currentImport = null;
			blockChanges = false;
			return;
		}
		/*
		 * if (currentImport != null && !iimport.equals(currentImport) &&
		 * !isReadOnly()) { commitChanges(false); }
		 */
		currentImport = iimport;
		/*
		 * if (currentImport instanceof IPluginImport) { IPluginImport pimport =
		 * (IPluginImport) currentImport;
		 * optionalButton.setEnabled(!isReadOnly());
		 * optionalButton.setSelection(pimport.isOptional());
		 * reexportButton.setEnabled(!isReadOnly());
		 * reexportButton.setSelection(pimport.isReexported()); }
		 * versionText.getControl().setEditable(!isReadOnly());
		 * versionText.setValue(currentImport.getVersion());
		 * resetMatchCombo(currentImport); blockChanges = false;
		 */
	}
}
