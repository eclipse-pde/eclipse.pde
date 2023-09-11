/*******************************************************************************
 * Copyright (c) 2011, 2022 Manumitting Technologies, Inc.
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
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.css.core.dom.ExtendedDocumentCSS;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
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
import org.w3c.css.sac.CSSParseException;
import org.w3c.dom.stylesheets.StyleSheet;
import org.w3c.dom.stylesheets.StyleSheetList;

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

		StringBuilder sb = new StringBuilder();

		// FIXME: expose these new protocols: resetCurrentTheme() and
		// getCSSEngines()
		((ThemeEngine) themeEngine).resetCurrentTheme();

		int count = 0;
		for (CSSEngine engine : ((ThemeEngine) themeEngine).getCSSEngines()) {
			if (count++ > 0) {
				sb.append("\n\n"); //$NON-NLS-1$
			}
			sb.append(MessageFormat.format(Messages.CSSScratchPadPart_Engine, engine.getClass().getSimpleName()));
			ExtendedDocumentCSS doc = (ExtendedDocumentCSS) engine.getDocumentCSS();
			List<StyleSheet> sheets = new ArrayList<>();
			StyleSheetList list = doc.getStyleSheets();
			for (int i = 0; i < list.getLength(); i++) {
				sheets.add(list.item(i));
			}

			try {
				Reader reader = new StringReader(cssText.getText());
				sheets.add(0, engine.parseStyleSheet(reader));
				doc.removeAllStyleSheets();
				for (StyleSheet sheet : sheets) {
					doc.addStyleSheet(sheet);
				}
				engine.reapply();

				long nanoDiff = System.nanoTime() - start;
				sb.append(MessageFormat.format("\n{0}", MessageFormat.format(Messages.CSSScratchPadPart_Time_ms, nanoDiff / 1000000))); //$NON-NLS-1$
			} catch (CSSParseException e) {
				sb.append(MessageFormat.format("\n{0}", MessageFormat.format(Messages.CSSScratchPadPart_Error_line_col, e.getLineNumber(), e.getColumnNumber(), e.getLocalizedMessage()))); //$NON-NLS-1$
			} catch (IOException e) {
				sb.append(MessageFormat.format("\n{0}", MessageFormat.format(Messages.CSSScratchPadPart_Error, e.getLocalizedMessage()))); //$NON-NLS-1$
			}
		}
		exceptions.setText(sb.toString());
	}

}
