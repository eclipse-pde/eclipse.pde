/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.launcher;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class ShowBundlesDialog extends Dialog {
	private Text fFilterText;
	private StyledText fModuleArgumentsText;
	private final Map<IPluginModelBase, String> fModelsWithStartLevels;
	private List<BundleLine> fAllLines;


	protected ShowBundlesDialog(Shell parentShell, Map<IPluginModelBase, String> modelsWithLevels) {
		super(parentShell);
		fModelsWithStartLevels = modelsWithLevels;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(PDEUIMessages.ShowBundlesDialog_LaunchBundles);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				PDEUIMessages.ShowBundlesDialog_Copy, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				PDEUIMessages.ShowBundlesDialog_Close, false);
	}

	@Override
	protected Rectangle getConstrainedShellBounds(Rectangle preferredSize) {
		Rectangle result = super.getConstrainedShellBounds(preferredSize);
		int heightLimit = convertHeightInCharsToPixels(40);
		int widthLimit = convertWidthInCharsToPixels(150);
		if (result.height > heightLimit) {
			result.y += (result.height - heightLimit) / 2;
			result.height = heightLimit;
		}
		if (result.width > widthLimit) {
			result.x += (result.width - widthLimit) / 2;
			result.width = widthLimit;
		}
		return result;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		comp.setLayout(new GridLayout());

		Label explanation = new Label(comp, SWT.WRAP);
		explanation.setText(PDEUIMessages.ShowBundlesDialog_Explanation);
		explanation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fFilterText = new Text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
		fFilterText.setMessage(PDEUIMessages.ShowBundlesDialog_FilterMessage);
		fFilterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fModuleArgumentsText = new StyledText(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fModuleArgumentsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fModuleArgumentsText.setEditable(false);

		fAllLines = createBundleLines();
		fFilterText.addModifyListener(e -> updateBundleText());
		updateBundleText();

		return comp;
	}

	private List<BundleLine> createBundleLines() {
		Set<String> duplicateBundles = new HashSet<>();
		Set<String> seenBundles = new HashSet<>();
		for (IPluginModelBase model : fModelsWithStartLevels.keySet()) {
			BundleDescription bundle = model.getBundleDescription();
			String symbolicName = bundle.getSymbolicName();
			if (!seenBundles.add(symbolicName)) {
				duplicateBundles.add(symbolicName);
			}
		}

		List<BundleLine> lines = fModelsWithStartLevels.entrySet().stream().map(entry -> {
			IPluginModelBase model = entry.getKey();
			BundleDescription bundle = model.getBundleDescription();
			String symbolicName = bundle.getSymbolicName();
			String startLevel = entry.getValue().substring(0, entry.getValue().indexOf(':'));
			String line = symbolicName + ", " + bundle.getVersion() + ", " + startLevel + ", " + model.getInstallLocation(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return new BundleLine(line, duplicateBundles.contains(symbolicName));
		}).toList();
		lines = new ArrayList<>(lines);
		Collections.sort(lines, (left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.text(), right.text()));
		return lines;
	}

	private void updateBundleText() {
		String filter = fFilterText.getText().trim().toLowerCase(Locale.ROOT);
		StringBuilder text = new StringBuilder();
		List<StyleRange> styleRanges = new ArrayList<>();
		for (BundleLine line : fAllLines) {
			if (filter.isEmpty() || line.text.toLowerCase(Locale.ROOT).contains(filter)) {
				if (text.length() > 0) {
					text.append('\n');
				}
				int offset = text.length();
				text.append(line.text);
				if (line.duplicate) {
					styleRanges.add(new StyleRange(offset, line.text.length(), null, null, SWT.BOLD));
				}
			}
		}
		fModuleArgumentsText.setText(text.toString());
		fModuleArgumentsText.setStyleRanges(styleRanges.toArray(StyleRange[]::new));
	}

	private record BundleLine(String text, boolean duplicate) {
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			Clipboard clipboard = new Clipboard(null);
			try {
				Transfer[] transfers = { TextTransfer.getInstance() };
				Object[] data = { fModuleArgumentsText.getText() };
				clipboard.setContents(data, transfers);
			} finally {
				clipboard.dispose();
			}
		}
		super.buttonPressed(buttonId);
	}

}
