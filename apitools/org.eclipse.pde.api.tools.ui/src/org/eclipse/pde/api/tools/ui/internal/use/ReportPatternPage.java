/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.use;

import java.util.regex.PatternSyntaxException;

import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.use.ApiUsePatternTab.Pattern;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ReportPatternPage extends UsePatternPage {

	static final String PAGE_NAME = "report"; //$NON-NLS-1$

	private Text patterntext = null;
	int kind = Pattern.REPORT_TO;
	Button to = null;
	Button from = null;
	String pattern = null;

	/**
	 * Constructor
	 *
	 * @param pattern
	 */
	public ReportPatternPage(String pattern, int kind) {
		super(PAGE_NAME, Messages.ReportPatternPage_report_conversion_pattern, null);
		this.pattern = pattern;
		this.kind = kind;
	}

	@Override
	public int getKind() {
		return this.kind;
	}

	@Override
	public String getPattern() {
		return this.patterntext.getText().trim();
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		this.to = SWTFactory.createRadioButton(comp, Messages.ReportPatternPage_filter_to_pattern);
		this.to.addSelectionListener(
				SelectionListener.widgetSelectedAdapter(e -> ReportPatternPage.this.kind = Pattern.REPORT_TO));
		this.from = SWTFactory.createRadioButton(comp, Messages.ReportPatternPage_filter_from_pattern);
		this.from.addSelectionListener(
				SelectionListener.widgetSelectedAdapter(e -> ReportPatternPage.this.kind = Pattern.REPORT));
		if (this.kind == Pattern.REPORT) {
			this.from.setSelection(true);
		} else {
			this.to.setSelection(true);
			this.kind = Pattern.REPORT_TO;
		}
		SWTFactory.createLabel(comp, Messages.ReportPatternPage_pattern, 1);
		this.patterntext = SWTFactory.createSingleText(comp, 1);
		if (this.pattern != null) {
			this.patterntext.setText(this.pattern);
		}
		this.patterntext.addModifyListener(e -> {
			setDirty();
			setPageComplete(isPageComplete());
		});
		this.patterntext.selectAll();
		this.patterntext.setFocus();
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IApiToolsHelpContextIds.APITOOLS_REPORT_PATTERN_WIZARD_PAGE);
	}

	@Override
	public boolean isPageComplete() {
		String newtext = this.patterntext.getText().trim();
		if (IApiToolsConstants.EMPTY_STRING.equals(newtext)) {
			if (pageDirty()) {
				setErrorMessage(Messages.ReportPatternPage_enter_conversion_pattern);
			} else {
				setMessage(Messages.ReportPatternPage_enter_conversion_pattern);
			}
			return false;
		}
		try {
			java.util.regex.Pattern.compile(newtext);
		} catch (PatternSyntaxException pse) {
			setErrorMessage(pse.getMessage());
			return false;
		}
		resetMessage(this.pattern != null);
		return true;
	}

	@Override
	protected void resetMessage(boolean isediting) {
		setErrorMessage(null);
		if (isediting) {
			setMessage(Messages.ReportPatternPage_edit_conversion_pattern);
		} else {
			setMessage(Messages.ReportPatternPage_create_conversion_pattern);
		}
	}
}
