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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.use.ApiUsePatternTab.Pattern;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for creating a RegEx pattern for augmenting a given page name in
 * an API description.
 *
 * @since 1.0.1
 */
public class DescriptionPatternPage extends UsePatternPage {

	static final String PAGE_NAME = "description"; //$NON-NLS-1$

	private int kind = -1;
	private Button kbutton = null;
	private Text patterntext = null;
	private String pattern = null;

	/**
	 * Constructor
	 *
	 * @param pattern
	 * @param kind
	 */
	public DescriptionPatternPage(String pattern, int kind) {
		super(PAGE_NAME, Messages.DescriptionPatternPage_package_name_pattern, null);
		this.pattern = pattern;
		resetMessage(this.pattern != null);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(comp, Messages.DescriptionPatternPage_patetern, 1);
		this.patterntext = SWTFactory.createSingleText(comp, 1);
		this.patterntext.addModifyListener(e -> {
			setDirty();
			setPageComplete(isPageComplete());
		});
		if (this.pattern != null) {
			this.patterntext.setText(this.pattern);
		}
		this.patterntext.selectAll();
		this.patterntext.setFocus();
		this.kbutton = SWTFactory.createCheckButton(comp, Messages.DescriptionPatternPage_api_pattern, null, this.kind == Pattern.API || this.kind == -1, 1);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IApiToolsHelpContextIds.APITOOLS_DESCRIPTION_PATTERN_WIZARD_PAGE);
	}

	@Override
	public boolean isPageComplete() {
		String newtext = this.patterntext.getText().trim();
		if (IApiToolsConstants.EMPTY_STRING.equals(newtext)) {
			if (pageDirty()) {
				setErrorMessage(Messages.DescriptionPatternPage_provide_regex);
			} else {
				setMessage(Messages.DescriptionPatternPage_provide_regex);
			}
			return false;
		}
		try {
			java.util.regex.Pattern.compile(newtext);
		} catch (PatternSyntaxException pse) {
			setErrorMessage(pse.getDescription());
			return false;
		}
		resetMessage(this.pattern != null);
		return true;
	}

	@Override
	protected void resetMessage(boolean isediting) {
		setErrorMessage(null);
		if (isediting) {
			setMessage(Messages.DescriptionPatternPage_edit_package_pattern);
		} else {
			setMessage(Messages.DescriptionPatternPage_create_package__pattern);
		}
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}

	@Override
	public int getKind() {
		return this.kbutton.getSelection() ? Pattern.API : Pattern.INTERNAL;
	}

	@Override
	public String getPattern() {
		return this.patterntext.getText().trim();
	}
}
