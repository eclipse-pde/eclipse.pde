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
package org.eclipse.pde.internal.ui.editor.site;

import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.feature.Choice;
import org.eclipse.pde.internal.ui.editor.feature.PortabilityChoicesDialog;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PortabilitySection extends PDESection implements IFormPart,
		IPartSelectionListener {
	private static final String DIALOG_TITLE = "SiteEditor.PortabilityChoicesDialog.title"; //$NON-NLS-1$

	private static final String SECTION_ARCH = "SiteEditor.PortabilitySection.arch"; //$NON-NLS-1$

	private static final String SECTION_DESC = "SiteEditor.PortabilitySection.desc"; //$NON-NLS-1$

	private static final String SECTION_EDIT = "SiteEditor.PortabilitySection.edit"; //$NON-NLS-1$

	private static final String SECTION_NL = "SiteEditor.PortabilitySection.nl"; //$NON-NLS-1$

	private static final String SECTION_OS = "SiteEditor.PortabilitySection.os"; //$NON-NLS-1$

	private static final String SECTION_TITLE = "SiteEditor.PortabilitySection.title"; //$NON-NLS-1$

	private static final String SECTION_WS = "SiteEditor.PortabilitySection.ws"; //$NON-NLS-1$

	public static Choice[] getArchChoices() {
		return getKnownChoices(Platform.knownOSArchValues());
	}

	private static Choice[] getKnownChoices(String[] values) {
		Choice[] choices = new Choice[values.length];
		for (int i = 0; i < choices.length; i++) {
			choices[i] = new Choice(values[i], values[i]);
		}
		return choices;
	}

	public static Choice[] getNLChoices() {
		Locale[] locales = Locale.getAvailableLocales();
		Choice[] choices = new Choice[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			choices[i] = new Choice(locale.toString(), locale.toString()
					+ " - " + locale.getDisplayName()); //$NON-NLS-1$
		}
		return choices;
	}

	public static Choice[] getOSChoices() {
		return getKnownChoices(Platform.knownOSValues());
	}

	public static Choice[] getWSChoices() {
		return getKnownChoices(Platform.knownWSValues());
	}

	private FormEntry archText;

	private ISiteFeature currentSiteFeature;

	private FormEntry nlText;

	private FormEntry osText;

	private FormEntry wsText;

	public PortabilitySection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEPlugin.getResourceString(SECTION_TITLE),
				PDEPlugin.getResourceString(SECTION_DESC), SWT.NULL);
	}

	public PortabilitySection(PDEFormPage page, Composite parent, String title,
			String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyValue(String property, String value) throws CoreException {
		if (currentSiteFeature == null)
			return;
		if (property.equals(IFeature.P_NL))
			currentSiteFeature.setNL(value);
		else if (property.equals(IEnvironment.P_OS))
			currentSiteFeature.setOS(value);
		else if (property.equals(IEnvironment.P_WS))
			currentSiteFeature.setWS(value);
		else if (property.equals(IEnvironment.P_ARCH))
			currentSiteFeature.setArch(value);
	}

	public void cancelEdit() {
		osText.cancelEdit();
		wsText.cancelEdit();
		nlText.cancelEdit();
		archText.cancelEdit();
		super.cancelEdit();
	}

	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(),
				RTFTransfer.getInstance() };
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

	private void clearField(String property) {
		if (property.equals(IEnvironment.P_OS))
			osText.setValue(null, true);
		else if (property.equals(IEnvironment.P_WS))
			wsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_ARCH))
			archText.setValue(null, true);
	}

	private void clearFields() {
		osText.setValue(null, true);
		wsText.setValue(null, true);
		if (nlText != null)
			nlText.setValue(null, true);
		archText.setValue(null, true);
	}

	public void commit(boolean onSave) {
		osText.commit();
		wsText.commit();
		if (nlText != null)
			nlText.commit();
		archText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		String editLabel = PDEPlugin.getResourceString(SECTION_EDIT);

		osText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_OS), editLabel, false);
		osText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(osText.getText().getDisplay(),
						new Runnable() {
							public void run() {
								Choice[] choices = getOSChoices();
								openPortabilityChoiceDialog(osText, choices);
							}
						});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_OS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(osText);

		wsText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_WS), editLabel, false);
		wsText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(wsText.getText().getDisplay(),
						new Runnable() {
							public void run() {
								Choice[] choices = getWSChoices();
								openPortabilityChoiceDialog(wsText, choices);
							}
						});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_WS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(wsText);

		nlText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_NL), editLabel, false);

		nlText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(nlText.getText().getDisplay(),
						new Runnable() {
							public void run() {
								Choice[] choices = getNLChoices();
								openPortabilityChoiceDialog(nlText, choices);
							}
						});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IFeature.P_NL, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(nlText);

		archText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_ARCH), editLabel, false);
		archText.setFormEntryListener(new FormEntryAdapter(this) {

			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(archText.getText().getDisplay(),
						new Runnable() {
							public void run() {
								Choice[] choices = getArchChoices();
								openPortabilityChoiceDialog(archText, choices);
							}
						});
			}

			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_ARCH, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		});
		limitTextWidth(archText);
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	private void enableForInput(boolean enable) {
		osText.getText().setEditable(enable);
		wsText.getText().setEditable(enable);
		if (nlText != null)
			nlText.getText().setEditable(enable);
		archText.getText().setEditable(enable);
		osText.getButton().setEnabled(enable);
		wsText.getButton().setEnabled(enable);
		if (nlText != null)
			nlText.getButton().setEnabled(enable);
		archText.getButton().setEnabled(enable);
	}

	public void initialize() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		enableForInput(model.isEditable());
		refresh();
		model.addModelChangedListener(this);
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	private void openPortabilityChoiceDialog(FormEntry text, Choice[] choices) {
		String value = text.getValue();

		PortabilityChoicesDialog dialog = new PortabilityChoicesDialog(
				PDEPlugin.getActiveWorkbenchShell(), choices, value);
		dialog.create();
		dialog.getShell().setText(PDEPlugin.getResourceString(DIALOG_TITLE));
		// dialog.getShell().setSize(300, 400);
		int result = dialog.open();
		if (result == Window.OK) {
			value = dialog.getValue();
			text.setValue(value);
		}
	}

	public void refresh() {
		if (currentSiteFeature == null) {
			clearFields();
			enableForInput(false);
			super.refresh();
			return;
		}
		enableForInput(true);
		setValue(IEnvironment.P_OS);
		setValue(IEnvironment.P_WS);
		setValue(IEnvironment.P_ARCH);
		if (nlText != null)
			setValue(IFeature.P_NL);
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof SiteFeatureAdapter) {
				currentSiteFeature = ((SiteFeatureAdapter) o).feature;
			} else {
				currentSiteFeature = null;
			}
		} else
			currentSiteFeature = null;
		refresh();
	}

	public void setFocus() {
		if (osText != null)
			osText.getText().setFocus();
	}

	private void setValue(String property) {
		if (currentSiteFeature == null) {
			clearField(property);
		} else {
			if (property.equals(IFeature.P_NL))
				nlText.setValue(currentSiteFeature.getNL(), true);
			else if (property.equals(IEnvironment.P_OS))
				osText.setValue(currentSiteFeature.getOS(), true);
			else if (property.equals(IEnvironment.P_WS))
				wsText.setValue(currentSiteFeature.getWS(), true);
			else if (property.equals(IEnvironment.P_ARCH))
				archText.setValue(currentSiteFeature.getArch(), true);
		}
	}
}
