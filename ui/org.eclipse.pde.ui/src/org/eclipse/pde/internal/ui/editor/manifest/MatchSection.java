package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.update.ui.forms.internal.*;

public class MatchSection extends PDEFormSection {
	private FormEntry versionText;
	private Button reexportButton;
	private CCombo matchCombo;
	private IPluginReference currentImport;
	private IStructuredSelection multiSelection;
	private boolean blockChanges = false;
	private boolean ignoreModelEvents = false;
	private boolean addReexport = true;
	public static final String SECTION_TITLE =
		"ManifestEditor.MatchSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.MatchSection.desc";
	public static final String KEY_REEXPORT =
		"ManifestEditor.MatchSection.reexport";
	public static final String KEY_VERSION =
		"ManifestEditor.MatchSection.version";
	public static final String KEY_RULE = "ManifestEditor.MatchSection.rule";
	public static final String KEY_NONE = "ManifestEditor.MatchSection.none";
	public static final String KEY_PERFECT =
		"ManifestEditor.MatchSection.perfect";
	public static final String KEY_EQUIVALENT =
		"ManifestEditor.MatchSection.equivalent";
	public static final String KEY_COMPATIBLE =
		"ManifestEditor.MatchSection.compatible";
	public static final String KEY_GREATER =
		"ManifestEditor.MatchSection.greater";
	public static final String KEY_VERSION_FORMAT =
		"ManifestEditor.PluginSpecSection.versionFormat";
	public static final String KEY_VERSION_TITLE =
		"ManifestEditor.PluginSpecSection.versionTitle";

	public MatchSection(PDEFormPage formPage, boolean addReexport) {
		super(formPage);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		this.addReexport = addReexport;
	}

	public MatchSection(PDEFormPage formPage) {
		this(formPage, true);
	}

	public void commitChanges(boolean onSave) {
		if (isDirty() == false)
			return;
		ignoreModelEvents = true;
		if ((currentImport != null || multiSelection != null)
			&& versionText.getControl().isEnabled()) {
			versionText.commit();
			String value = versionText.getValue();
			int match = IPluginImport.NONE;
			if (value != null && value.length() > 0) {
				applyVersion(value);
				match = getMatch();
			}
			applyMatch(match);
		}
		setDirty(false);
		ignoreModelEvents = false;
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.marginWidth = layout.marginHeight = 2;
		container.setLayout(layout);
		GridData gd;

		if (addReexport) {
			reexportButton =
				factory.createButton(
					container,
					PDEPlugin.getResourceString(KEY_REEXPORT),
					SWT.CHECK);
			reexportButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (blockChanges)
						return;
					if (!(currentImport instanceof IPluginImport))
						return;
					if (currentImport != null) {
						try {
							IPluginImport iimport =
								(IPluginImport) currentImport;
							ignoreModelEvents = true;
							iimport.setReexported(
								reexportButton.getSelection());
							ignoreModelEvents = false;
						} catch (CoreException ex) {
							PDEPlugin.logException(ex);
						}
					}
				}
			});
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			reexportButton.setLayoutData(gd);
		}

		versionText =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(KEY_VERSION),
					factory));
		versionText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					ignoreModelEvents = true;
					if (value != null && value.length() > 0) {
						PluginVersionIdentifier pvi =
							new PluginVersionIdentifier(text.getValue());
						String formatted = pvi.toString();
						text.setValue(formatted, true);
						applyVersion(formatted);
					} else {
						applyVersion(null);
					}
					ignoreModelEvents = false;
				} catch (Throwable e) {
					String message =
						PDEPlugin.getResourceString(KEY_VERSION_FORMAT);
					MessageDialog.openError(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString(KEY_VERSION_TITLE),
						message);
					text.setValue(currentImport.getVersion(), true);
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

		matchCombo = new CCombo(container, SWT.READ_ONLY | SWT.FLAT);
		matchCombo.add(PDEPlugin.getResourceString(KEY_NONE));
		matchCombo.add(PDEPlugin.getResourceString(KEY_EQUIVALENT));
		matchCombo.add(PDEPlugin.getResourceString(KEY_COMPATIBLE));
		matchCombo.add(PDEPlugin.getResourceString(KEY_PERFECT));
		matchCombo.add(PDEPlugin.getResourceString(KEY_GREATER));
		matchCombo.pack();
		matchCombo.setBackground(factory.getBackgroundColor());
		matchCombo.setForeground(factory.getForegroundColor());
		matchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		matchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!blockChanges) {
					applyMatch(matchCombo.getSelectionIndex());
				}
			}
		});

		factory.paintBordersFor(container);

		update((IPluginReference) null);
		return container;
	}

	private void applyVersion(String version) {
		try {
			if (currentImport != null) {
				currentImport.setVersion(version);
			} else if (multiSelection != null) {
				for (Iterator iter = multiSelection.iterator();
					iter.hasNext();
					) {
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
				for (Iterator iter = multiSelection.iterator();
					iter.hasNext();
					) {
					IPluginReference reference = (IPluginReference) iter.next();
					reference.setMatch(match);
				}
			}
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}

	private void forceDirty() {
		setDirty(true);
		IModel model = (IModel) getFormPage().getModel();
		if (model instanceof IEditable) {
			IEditable editable = (IEditable) model;
			editable.setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	private int getMatch() {
		return matchCombo.getSelectionIndex();
	}

	private void buttonChanged(Button radio) {
		if (currentImport == null || blockChanges)
			return;
		forceDirty();
	}

	public boolean canPaste(Clipboard clipboard) {
		return (clipboard.getContents(TextTransfer.getInstance()) != null);
	}
	public void dispose() {
		IModel model = (IModel) getFormPage().getModel();
		if (model instanceof IModelChangeProvider)
			 ((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}
	private void fillContextMenu(IMenuManager manager) {
		((DependenciesForm) getFormPage().getForm()).fillContextMenu(manager);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	public void initialize(Object input) {
		IModel model = (IModel) input;
		setReadOnly(!model.isEditable());
		if (model instanceof IModelChangeProvider)
			 ((IModelChangeProvider) model).addModelChangedListener(this);
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

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		IPluginReference input = null;
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
		String text = versionText.getControl().getText();
		boolean enable = !isReadOnly() && text.length() > 0;
		matchCombo.setEnabled(enable);
		setMatchCombo(iimport);
	}

	private void setMatchCombo(IPluginReference iimport) {
		int match = iimport != null ? iimport.getMatch() : IMatchRules.NONE;
		matchCombo.select(match);
	}

	private void update(IStructuredSelection selection) {
		blockChanges = true;
		currentImport = null;
		int size = selection.size();
		if (size == 0) {
			versionText.setValue(null, true);
			boolean enableState = false;
			versionText.getControl().setEditable(enableState);
			matchCombo.setEnabled(enableState);
			matchCombo.setText("");
			blockChanges = false;
			return;
		}
		if (multiSelection != null
			&& !multiSelection.equals(selection)
			&& !isReadOnly()) {
			commitChanges(false);
		}
		multiSelection = selection;
		versionText.getControl().setEditable(!isReadOnly());

		if (size == 1) {
			IPluginReference ref =
				(IPluginReference) selection.getFirstElement();
			versionText.setValue(ref.getVersion());
			resetMatchCombo(ref);
		} else {
			versionText.setValue("");
			matchCombo.setEnabled(true);
			setMatchCombo(null);
		}
		blockChanges = false;
	}

	private void update(IPluginReference iimport) {
		blockChanges = true;
		if (iimport == null) {
			if (addReexport) {
				reexportButton.setSelection(false);
				reexportButton.setEnabled(false);
			}
			versionText.setValue(null, true);
			boolean enableState = false;
			versionText.getControl().setEditable(enableState);
			matchCombo.setEnabled(enableState);
			matchCombo.setText("");
			currentImport = null;
			blockChanges = false;
			return;
		}
		if (currentImport != null
			&& !iimport.equals(currentImport)
			&& !isReadOnly()) {
			commitChanges(false);
		}
		currentImport = iimport;
		if (currentImport instanceof IPluginImport) {
			IPluginImport pimport = (IPluginImport) currentImport;
			reexportButton.setEnabled(!isReadOnly());
			reexportButton.setSelection(pimport.isReexported());
		}
		versionText.getControl().setEditable(!isReadOnly());
		versionText.setValue(currentImport.getVersion());
		resetMatchCombo(currentImport);
		blockChanges = false;
	}
}