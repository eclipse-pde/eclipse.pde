/*******************************************************************************
 * Copyright (c) 2011, 2026 Manumitting Technologies, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.spy.css;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.core.impl.dom.CSSStyleSheetImpl;
import org.eclipse.e4.ui.css.core.impl.parser.CssParseException;
import org.eclipse.e4.ui.css.swt.helpers.EclipsePreferencesHelper;
import org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

@SuppressWarnings("restriction")
public class CSSScratchPadPart {
	@Inject
	@Optional
	private IThemeEngine themeEngine;

	private static final int APPLY_ID = IDialogConstants.OK_ID + 100;
	/**
	 * Collection of buttons created by the <code>createButton</code> method.
	 */
	private final HashMap<Integer, Button> buttons = new HashMap<>();

	private Text cssText;
	private Text exceptions;

	@PostConstruct
	protected Control createDialogArea(Composite parent) {

		Composite outer = parent;
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(GridData.FILL_BOTH));

		SashForm sashForm = new SashForm(outer, SWT.VERTICAL);

		cssText = new Text(sashForm, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);

		exceptions = new Text(sashForm, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);
		sashForm.setWeights(80, 20);

		createButtonsForButtonBar(parent);
		return outer;
	}

	private void createButtonsForButtonBar(Composite parent) {
		createButton(parent, APPLY_ID, Messages.CSSScratchPadPart_Apply, true);
		createButton(parent, IDialogConstants.OK_ID, Messages.CSSScratchPadPart_Close, false);
		// createButton(parent, IDialogConstants.CANCEL_ID,
		// IDialogConstants.CANCEL_LABEL, false);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setFont(JFaceResources.getDialogFont());
		button.setData(Integer.valueOf(id));
		button.addSelectionListener(SelectionListener
				.widgetSelectedAdapter(event -> buttonPressed(((Integer) event.widget.getData()).intValue())));
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		buttons.put(Integer.valueOf(id), button);
		// setButtonLayoutData(button);
		return button;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case APPLY_ID:
			applyCSS();
			break;
		default:
			break;
		}
	}

	private void applyCSS() {
		if (themeEngine == null) {
			exceptions.setText(Messages.CSSScratchPadPart_No_theme_engine_available);
			return;
		}
		long start = System.nanoTime();
		exceptions.setText(""); //$NON-NLS-1$

		// FIXME: expose these new protocols: resetCurrentTheme() and
		// getCSSEngines()
		((ThemeEngine) themeEngine).resetCurrentTheme();

		CSSStyleSheetImpl styleSheet = null;
		int changedPreferences = 0;
		try {
			for (CSSEngine engine : ((ThemeEngine) themeEngine).getCSSEngines()) {
				// Appended last, so the scratch sheet wins cascade ties.
				styleSheet = engine.parseStyleSheet(new StringReader(cssText.getText()));
				engine.reapply();
				changedPreferences += applyToPreferences(engine);
			}
		} catch (IOException | RuntimeException e) {
			exceptions.setText(MessageFormat.format(Messages.CSSScratchPadPart_Error, e.getLocalizedMessage()));
			return;
		}

		long millis = (System.nanoTime() - start) / 1_000_000;
		int ruleCount = styleSheet == null ? 0 : styleSheet.getRules().size();
		StringBuilder sb = new StringBuilder(
				MessageFormat.format(Messages.CSSScratchPadPart_Summary, ruleCount, changedPreferences, millis));
		if (styleSheet != null) {
			// The parser drops malformed rules and keeps going, so they never reach the catch above
			for (CssParseException problem : styleSheet.getProblems()) {
				sb.append('\n').append(MessageFormat.format(Messages.CSSScratchPadPart_Skipped, problem.getMessage()));
			}
		}
		exceptions.setText(sb.toString());
	}

	// Same nodes the workbench styles on a theme change
	private static int applyToPreferences(CSSEngine engine) {
		int changed = 0;
		for (String bundleId : getThemeRelatedBundleIds()) {
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(bundleId);
			Map<String, String> before = new HashMap<>();
			// CSS only overwrites values it did not write before unless the theme changed
			for (String name : EclipsePreferencesHelper.getOverriddenPropertyNames(preferences)) {
				before.put(name, preferences.get(name, null));
				preferences.remove(name);
			}
			engine.applyStyles(preferences, false);
			Set<String> names = new HashSet<>(before.keySet());
			names.addAll(EclipsePreferencesHelper.getOverriddenPropertyNames(preferences));
			for (String name : names) {
				if (!Objects.equals(before.get(name), preferences.get(name, null))) {
					changed++;
				}
			}
		}
		return changed;
	}

	private static Set<String> getThemeRelatedBundleIds() {
		Set<String> bundleIds = new TreeSet<>();
		for (String extensionPoint : new String[] { "org.eclipse.e4.ui.css.swt.theme", "org.eclipse.ui.themes" }) { //$NON-NLS-1$ //$NON-NLS-2$
			for (IConfigurationElement element : Platform.getExtensionRegistry()
					.getConfigurationElementsFor(extensionPoint)) {
				bundleIds.add(element.getNamespaceIdentifier());
			}
		}
		return bundleIds;
	}

}
