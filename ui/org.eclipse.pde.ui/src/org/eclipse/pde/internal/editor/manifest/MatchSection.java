package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.model.ImportObject;
import org.eclipse.pde.model.*;

public class MatchSection extends PDEFormSection {
	private FormEntry versionText;
	private Button reexportButton;
	private Button perfectButton;
	private Button equivButton;
	private Button compatibleButton;
	private Button greaterButton;
	private IPluginReference currentImport;
	private boolean blockChanges = false;
	private boolean ignoreModelEvents = false;
	private boolean addReexport = true;
	public static final String SECTION_TITLE = "ManifestEditor.MatchSection.title";
	public static final String SECTION_DESC = "ManifestEditor.MatchSection.desc";
	public static final String KEY_REEXPORT =
		"ManifestEditor.MatchSection.reexport";
	public static final String KEY_VERSION = "ManifestEditor.MatchSection.version";
	public static final String KEY_RULE = "ManifestEditor.MatchSection.rule";
	public static final String KEY_PERFECT = "ManifestEditor.MatchSection.perfect";
	public static final String KEY_EQUIVALENT =
		"ManifestEditor.MatchSection.equivalent";
	public static final String KEY_COMPATIBLE =
		"ManifestEditor.MatchSection.compatible";
	public static final String KEY_GREATER = "ManifestEditor.MatchSection.greater";
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
		if (currentImport != null && versionText.getControl().isEnabled()) {
			versionText.commit();
			String value = versionText.getValue();
			int match = IPluginImport.NONE;
			try {
				if (value != null && value.length() > 0) {

					currentImport.setVersion(value);
					match = getMatch();
				}
				currentImport.setMatch(match);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		setDirty(false);
		ignoreModelEvents = false;
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
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
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			reexportButton.setLayoutData(gd);
		}

		versionText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(KEY_VERSION), factory));
		versionText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					ignoreModelEvents = true;
					if (value != null && value.length() > 0) {
						PluginVersionIdentifier pvi = new PluginVersionIdentifier(text.getValue());
						String formatted = pvi.toString();
						text.setValue(formatted, true);
						currentImport.setVersion(formatted);
					} else {
						currentImport.setVersion(null);
					}
					ignoreModelEvents = false;
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} catch (Throwable e) {
					String message = PDEPlugin.getResourceString(KEY_VERSION_FORMAT);
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
				resetRadioButtons(currentImport);
				blockChanges = false;
			}
		});
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				buttonChanged((Button) e.widget);
			}
		};

		perfectButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_PERFECT),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 15;
		perfectButton.setLayoutData(gd);
		perfectButton.addSelectionListener(listener);
		equivButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_EQUIVALENT),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 15;
		equivButton.setLayoutData(gd);
		equivButton.addSelectionListener(listener);
		compatibleButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_COMPATIBLE),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 15;
		compatibleButton.setLayoutData(gd);
		compatibleButton.addSelectionListener(listener);
		greaterButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_GREATER),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 15;
		greaterButton.setLayoutData(gd);
		greaterButton.addSelectionListener(listener);

		factory.paintBordersFor(container);

		update(null);
		return container;
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
		int match = IMatchRules.NONE;
		if (perfectButton.getSelection())
			match = IMatchRules.PERFECT;
		if (equivButton.getSelection())
			match = IMatchRules.EQUIVALENT;
		if (compatibleButton.getSelection())
			match = IMatchRules.COMPATIBLE;
		if (greaterButton.getSelection())
			match = IMatchRules.GREATER_OR_EQUAL;
		return match;
	}

	private void buttonChanged(Button radio) {
		if (currentImport == null || blockChanges)
			return;
		forceDirty();
	}

	public void dispose() {
		IModel model = (IModel) getFormPage().getModel();
		if (model instanceof IModelChangeProvider)
			 ((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}
	private void fillContextMenu(IMenuManager manager) {
		((DependenciesForm) getFormPage().getForm()).fillContextMenu(manager);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
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
				update(null);
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
		   input = (IPluginReference)changeObject;
		update(input);
	}

	private void resetRadioButtons(IPluginReference iimport) {
		String text = versionText.getControl().getText();
		boolean enable = !isReadOnly() && text.length() > 0;
		perfectButton.setEnabled(enable);
		equivButton.setEnabled(enable);
		compatibleButton.setEnabled(enable);
		greaterButton.setEnabled(enable);
		setRadioButtons(iimport);
	}

	private void setRadioButtons(IPluginReference iimport) {
		int match = iimport != null ? iimport.getMatch() : IMatchRules.NONE;
		perfectButton.setSelection(match == IMatchRules.PERFECT);
		equivButton.setSelection(match == IMatchRules.EQUIVALENT);
		compatibleButton.setSelection(match == IMatchRules.COMPATIBLE);
		greaterButton.setSelection(match == IMatchRules.GREATER_OR_EQUAL);
	}

	private void update(IPluginReference iimport) {
		blockChanges = true;
		if (iimport == null) {
			if (addReexport) {
				reexportButton.setSelection(false);
				reexportButton.setEnabled(false);
			}
			perfectButton.setSelection(false);
			perfectButton.setEnabled(false);
			equivButton.setSelection(false);
			equivButton.setEnabled(false);
			compatibleButton.setSelection(false);
			compatibleButton.setEnabled(false);
			greaterButton.setSelection(false);
			greaterButton.setEnabled(false);
			versionText.setValue(null, true);
			versionText.getControl().setEditable(false);
			currentImport = null;
			blockChanges = false;
			return;
		}
		if (currentImport != null && !iimport.equals(currentImport) && !isReadOnly()) {
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
		resetRadioButtons(currentImport);
		blockChanges = false;
	}
}