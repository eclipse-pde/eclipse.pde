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
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.ifeature.IFeature;
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
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class PortabilitySection extends PDESection {
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

	private FormEntry fNlText;

	private FormEntry fOsText;

	private FormEntry fWsText;

	public PortabilitySection(FeatureFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.FeatureEditor_PortabilitySection_title, PDEUIMessages.FeatureEditor_PortabilitySection_desc, SWT.NULL);
	}

	public PortabilitySection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyValue(String property, String value) throws CoreException {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		if (property.equals(IEnvironment.P_NL))
			feature.setNL(value);
		else if (property.equals(IEnvironment.P_OS))
			feature.setOS(value);
		else if (property.equals(IEnvironment.P_WS))
			feature.setWS(value);
		else if (property.equals(IEnvironment.P_ARCH))
			feature.setArch(value);
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

		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData twd = new TableWrapData();
		twd.grabHorizontal = true;
		section.setLayoutData(twd);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		String editLabel = PDEUIMessages.FeatureEditor_PortabilitySection_edit;

		fOsText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_PortabilitySection_os, editLabel, false);
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
		fOsText.setEditable(isEditable());

		fWsText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_PortabilitySection_ws, editLabel, false);
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
		fWsText.setEditable(isEditable());

		fNlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_PortabilitySection_nl, editLabel, false);

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
		fNlText.setEditable(isEditable());

		fArchText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_PortabilitySection_arch, editLabel, false);
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
		fArchText.setEditable(isEditable());

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
		dialog.getShell().setText(PDEUIMessages.FeatureEditor_PortabilityChoicesDialog_title);

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
		setValue(IEnvironment.P_OS);
		setValue(IEnvironment.P_WS);
		setValue(IEnvironment.P_ARCH);
		setValue(IEnvironment.P_NL);
		super.refresh();
	}

	@Override
	public void setFocus() {
		if (fOsText != null)
			fOsText.getText().setFocus();
	}

	private void setValue(String property) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		if (property.equals(IEnvironment.P_NL))
			fNlText.setValue(feature.getNL(), true);
		else if (property.equals(IEnvironment.P_OS))
			fOsText.setValue(feature.getOS(), true);
		else if (property.equals(IEnvironment.P_WS))
			fWsText.setValue(feature.getWS(), true);
		else if (property.equals(IEnvironment.P_ARCH))
			fArchText.setValue(feature.getArch(), true);
	}
}
