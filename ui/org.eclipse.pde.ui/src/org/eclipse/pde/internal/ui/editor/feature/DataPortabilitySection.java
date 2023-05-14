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
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DataPortabilitySection extends PDESection implements IPartSelectionListener {
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
			choices[i] = new Choice(locale.toString(), locale.toString() + " - " + locale.getDisplayName()); //$NON-NLS-1$
		}
		return choices;
	}

	public static Choice[] getOSChoices() {
		return getKnownChoices(Platform.knownOSValues());
	}

	public static Choice[] getWSChoices() {
		return getKnownChoices(Platform.knownWSValues());
	}

	private FormEntry fArchText;

	private IFeatureData fCurrentInput;

	private FormEntry fNlText;

	private FormEntry fOsText;

	private FormEntry fWsText;

	public DataPortabilitySection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.FeatureEditor_DataDetailsSection_title, PDEUIMessages.FeatureEditor_DataDetailsSection_desc, SWT.NULL);
	}

	public DataPortabilitySection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.NO_TITLE | toggleStyle, false);
		// getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyValue(String property, String value) throws CoreException {
		if (fCurrentInput == null)
			return;
		if (property.equals(IEnvironment.P_NL))
			fCurrentInput.setNL(value);
		else if (property.equals(IEnvironment.P_OS))
			fCurrentInput.setOS(value);
		else if (property.equals(IEnvironment.P_WS))
			fCurrentInput.setWS(value);
		else if (property.equals(IEnvironment.P_ARCH))
			fCurrentInput.setArch(value);
	}

	@Override
	public void cancelEdit() {
		fOsText.cancelEdit();
		fWsText.cancelEdit();
		fNlText.cancelEdit();
		fArchText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (TransferData type : types) {
			for (Transfer transfer : transfers) {
				if (transfer.isSupportedType(type))
					return true;
			}
		}
		return false;
	}

	private void clearField(String property) {
		if (property.equals(IEnvironment.P_OS))
			fOsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_WS))
			fWsText.setValue(null, true);
		else if (property.equals(IEnvironment.P_ARCH))
			fArchText.setValue(null, true);
		else if (property.equals(IEnvironment.P_NL))
			fNlText.setValue(null, true);
	}

	private void clearFields() {
		fOsText.setValue(null, true);
		fWsText.setValue(null, true);
		fNlText.setValue(null, true);
		fArchText.setValue(null, true);
	}

	@Override
	public void commit(boolean onSave) {
		fOsText.commit();
		fWsText.commit();
		fNlText.commit();
		fArchText.commit();
		super.commit(onSave);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		String editLabel = PDEUIMessages.SiteEditor_PortabilitySection_edit;

		fOsText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PortabilitySection_os, editLabel, false);
		fOsText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fOsText.getText().getDisplay(), () -> {
					Choice[] choices = getOSChoices();
					openPortabilityChoiceDialog(IEnvironment.P_OS, fOsText, choices);
				});
			}

			@Override
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_OS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fOsText);
		fOsText.setEditable(fCurrentInput != null && isEditable());

		fWsText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PortabilitySection_ws, editLabel, false);
		fWsText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fWsText.getText().getDisplay(), () -> {
					Choice[] choices = getWSChoices();
					openPortabilityChoiceDialog(IEnvironment.P_WS, fWsText, choices);
				});
			}

			@Override
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_WS, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fWsText);
		fWsText.setEditable(fCurrentInput != null && isEditable());

		fNlText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PortabilitySection_nl, editLabel, false);

		fNlText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fNlText.getText().getDisplay(), () -> {
					Choice[] choices = getNLChoices();
					openPortabilityChoiceDialog(IEnvironment.P_NL, fNlText, choices);
				});
			}

			@Override
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_NL, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fNlText);
		fNlText.setEditable(fCurrentInput != null && isEditable());

		fArchText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PortabilitySection_arch, editLabel, false);
		fArchText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void browseButtonSelected(FormEntry entry) {
				BusyIndicator.showWhile(fArchText.getText().getDisplay(), () -> {
					Choice[] choices = getArchChoices();
					openPortabilityChoiceDialog(IEnvironment.P_ARCH, fArchText, choices);
				});
			}

			@Override
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(IEnvironment.P_ARCH, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		});
		limitTextWidth(fArchText);
		fArchText.setEditable(fCurrentInput != null && isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	@Override
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public void initialize(IManagedForm form) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
		super.initialize(form);
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	private void openPortabilityChoiceDialog(String property, FormEntry text, Choice[] choices) {
		String value = text.getValue();

		PortabilityChoicesDialog dialog = new PortabilityChoicesDialog(PDEPlugin.getActiveWorkbenchShell(), choices, value);
		dialog.create();
		dialog.getShell().setText(PDEUIMessages.SiteEditor_PortabilityChoicesDialog_title);

		int result = dialog.open();
		if (result == Window.OK) {
			value = dialog.getValue();
			text.setValue(value);
			try {
				applyValue(property, value);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	@Override
	public void refresh() {
		if (fCurrentInput == null) {
			clearFields();
		} else {
			setValue(IEnvironment.P_OS);
			setValue(IEnvironment.P_WS);
			setValue(IEnvironment.P_ARCH);
			setValue(IEnvironment.P_NL);
		}

		fOsText.setEditable(fCurrentInput != null && isEditable());
		fWsText.setEditable(fCurrentInput != null && isEditable());
		fNlText.setEditable(fCurrentInput != null && isEditable());
		fArchText.setEditable(fCurrentInput != null && isEditable());

		super.refresh();
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFeatureData) {
				fCurrentInput = (IFeatureData) o;
			} else {
				fCurrentInput = null;
			}
		} else
			fCurrentInput = null;
		refresh();
	}

	@Override
	public void setFocus() {
		if (fOsText != null)
			fOsText.getText().setFocus();
	}

	private void setValue(String property) {
		if (fCurrentInput == null) {
			clearField(property);
		} else {
			if (property.equals(IEnvironment.P_NL))
				fNlText.setValue(fCurrentInput.getNL(), true);
			else if (property.equals(IEnvironment.P_OS))
				fOsText.setValue(fCurrentInput.getOS(), true);
			else if (property.equals(IEnvironment.P_WS))
				fWsText.setValue(fCurrentInput.getWS(), true);
			else if (property.equals(IEnvironment.P_ARCH))
				fArchText.setValue(fCurrentInput.getArch(), true);
		}
	}
}
