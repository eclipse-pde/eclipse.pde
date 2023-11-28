/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.TreeSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section for editing OS, WS, Arch and NL settings in the target definition editor
 * @see EnvironmentPage
 * @see TargetEditor
 */
public class EnvironmentSection extends SectionPart {

	private ComboPart fOSCombo;
	private ComboPart fWSCombo;
	private ComboPart fNLCombo;
	private ComboPart fArchCombo;

	private TreeSet<String> fNLChoices;
	private TreeSet<String> fOSChoices;
	private TreeSet<String> fWSChoices;
	private TreeSet<String> fArchChoices;
	private boolean LOCALES_INITIALIZED = false;

	private final TargetEditor fEditor;

	public EnvironmentSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 *
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setText(PDEUIMessages.EnvironmentBlock_targetEnv);
		section.setDescription(PDEUIMessages.EnvironmentSection_description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(true, 2));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite left = toolkit.createComposite(client);
		left.setLayout(new GridLayout(2, false));
		GridLayout layout = FormLayoutFactory.createClearGridLayout(false, 2);
		layout.horizontalSpacing = layout.verticalSpacing = 5;
		left.setLayout(layout);
		left.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		initializeChoices();

		Label label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_operationSystem);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fOSCombo = new ComboPart();
		fOSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER);
		fOSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems(fOSChoices.toArray(new String[fOSChoices.size()]));
		fOSCombo.setVisibleItemCount(30);

		label = toolkit.createLabel(left, PDEUIMessages.EnvironmentSection_windowingSystem);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fWSCombo = new ComboPart();
		fWSCombo.createControl(left, toolkit, SWT.SINGLE | SWT.BORDER);
		fWSCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems(fWSChoices.toArray(new String[fWSChoices.size()]));
		fWSCombo.setVisibleItemCount(30);

		Composite right = toolkit.createComposite(client);
		layout = FormLayoutFactory.createClearGridLayout(false, 2);
		layout.verticalSpacing = layout.horizontalSpacing = 5;
		right.setLayout(layout);
		right.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_architecture);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fArchCombo = new ComboPart();
		fArchCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER);
		fArchCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems(fArchChoices.toArray(new String[fArchChoices.size()]));
		fArchCombo.setVisibleItemCount(30);

		label = toolkit.createLabel(right, PDEUIMessages.EnvironmentSection_locale);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fNLCombo = new ComboPart();
		fNLCombo.createControl(right, toolkit, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		fNLCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems(fNLChoices.toArray(new String[fNLChoices.size()]));
		fNLCombo.setVisibleItemCount(30);

		refresh();

		fOSCombo.addModifyListener(e -> {
			markDirty();
			getTarget().setOS(getText(fOSCombo));
		});
		fWSCombo.addModifyListener(e -> {
			markDirty();
			getTarget().setWS(getText(fWSCombo));
		});
		fArchCombo.addModifyListener(e -> {
			markDirty();
			getTarget().setArch(getText(fArchCombo));
		});
		fNLCombo.getControl().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent event) {
				// if we haven't gotten all the values for the NL's, display a busy cursor to the user while we find them.
				if (!LOCALES_INITIALIZED) {
					try {
						PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
							initializeAllLocales();
							LOCALES_INITIALIZED = true;
						});
					} catch (InvocationTargetException | InterruptedException e) {
						PDEPlugin.log(e);
					}
				}

				// first time through, we should have a max item count of 1.
				// On the first time through, we need to set the new values, and also attach the listener
				// If we attached the listener initially, when we call setItems(..), it would make the editor dirty (when the user didn't change anything)
				if (fNLCombo.getItemCount() < 3) {
					String current = fNLCombo.getSelection();
					if (!fNLCombo.getControl().isDisposed()) {
						fNLCombo.setItems(fNLChoices.toArray(new String[fNLChoices.size()]));
						fNLCombo.setText(current);
					}

					fNLCombo.addModifyListener(e -> {
						String value = getText(fNLCombo);
						if (value == null) {
							getTarget().setNL(null);
						} else {
							int index = value.indexOf('-');
							if (index > 0)
								value = value.substring(0, index);
							getTarget().setNL(value.trim());
						}
						markDirty();
					});
				}

			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	private void initializeChoices() {
		ITargetDefinition target = getTarget();
		fOSChoices = new TreeSet<>();
		String[] os = Platform.knownOSValues();
		Collections.addAll(fOSChoices, os);
		fOSChoices.add(""); //$NON-NLS-1$
		String fileValue = target.getOS();
		if (fileValue != null)
			fOSChoices.add(fileValue);

		fWSChoices = new TreeSet<>();
		String[] ws = Platform.knownWSValues();
		Collections.addAll(fWSChoices, ws);
		fWSChoices.add(""); //$NON-NLS-1$
		fileValue = target.getWS();
		if (fileValue != null)
			fWSChoices.add(fileValue);

		fArchChoices = new TreeSet<>();
		String[] arch = Platform.knownOSArchValues();
		Collections.addAll(fArchChoices, arch);
		fArchChoices.add(""); //$NON-NLS-1$
		fileValue = target.getArch();
		if (fileValue != null)
			fArchChoices.add(fileValue);

		fNLChoices = new TreeSet<>();
		fNLChoices.add(""); //$NON-NLS-1$
	}

	private void initializeAllLocales() {
		String[] nl = LocaleUtil.getLocales();
		Collections.addAll(fNLChoices, nl);
		String fileValue = getTarget().getNL();
		if (fileValue != null)
			fNLChoices.add(LocaleUtil.expandLocaleName(fileValue));
		LOCALES_INITIALIZED = true;
	}

	/**
	 * Returns the text of the widget or null if it is empty
	 * @return text of the widget or <code>null</code>
	 */
	private String getText(ComboPart combo) {
		String text;
		Control control = combo.getControl();
		if (control instanceof Combo) {
			text = ((Combo) control).getText();
		} else {
			text = ((CCombo) control).getText();
		}
		text = text.trim();
		if (text.length() == 0) {
			return null;
		}
		return text;
	}

	@Override
	public void refresh() {
		ITargetDefinition target = getTarget();
		String presetValue = (target.getOS() == null) ? "" : target.getOS(); //$NON-NLS-1$
		fOSCombo.setText(presetValue);
		presetValue = (target.getWS() == null) ? "" : target.getWS(); //$NON-NLS-1$
		fWSCombo.setText(presetValue);
		presetValue = (target.getArch() == null) ? "" : target.getArch(); //$NON-NLS-1$
		fArchCombo.setText(presetValue);
		presetValue = (target.getNL() == null) ? "" : LocaleUtil.expandLocaleName(target.getNL()); //$NON-NLS-1$
		fNLCombo.setText(presetValue);
		super.refresh();
	}

	protected void updateChoices() {
		if (LOCALES_INITIALIZED)
			return;
		// kick off thread in background to find the NL values
		new Thread(this::initializeAllLocales).start();
	}

}
