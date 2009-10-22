/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.use.ApiUsePatternTab.Pattern;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for creating a pattern to exclude nested jars from 
 * the scan.
 * 
 * @since 1.0.1
 */
public class ArchivePatternPage extends UsePatternPage {

	static final String PAGE_NAME = "archive"; //$NON-NLS-1$
	
	private Text bundletext = null, patterntext = null;
	private String bundle = null, archive = null;
	
	/**
	 * Constructor
	 * @param pattern
	 */
	protected ArchivePatternPage(String pattern) {
		super(PAGE_NAME, Messages.ArchivePatternPage_nested_archive_pattern, null);
		if(pattern != null) {
			String[] parts = pattern.split(":"); //$NON-NLS-1$
			if(parts.length == 2) {
				this.bundle = parts[0];
				this.archive = parts[1];
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel(comp, Messages.ArchivePatternPage_bundle_name, 1);
		this.bundletext = SWTFactory.createSingleText(comp, 1);
		if(this.bundle != null) {
			this.bundletext.setText(this.bundle);
		}
		this.bundletext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty();
				setPageComplete(isPageComplete());
			}
		});
		SWTFactory.createLabel(comp, Messages.ArchivePatternPage_archive_name, 1);
		this.patterntext = SWTFactory.createSingleText(comp, 1);
		if(this.archive != null) {
			this.patterntext.setText(this.archive);
		}
		this.patterntext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty();
				setPageComplete(isPageComplete());
			}
		});
		setControl(comp);
		this.bundletext.setFocus();
		this.bundletext.selectAll();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IApiToolsHelpContextIds.APITOOLS_ARCHIVE_PATTERN_WIZARD_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		if(this.bundletext.getText().trim().length() == 0) {
			if(pageDirty()) {
				setErrorMessage(Messages.ArchivePatternPage_enter_bundle_name);
			}
			else {
				setMessage(Messages.ArchivePatternPage_enter_bundle_name);
			}
			return false;
		}
		if(this.patterntext.getText().trim().length() == 0) {
			if(pageDirty()) {
				setErrorMessage(Messages.ArchivePatternPage_enter_a_pattern);
			}
			else {
				setMessage(Messages.ArchivePatternPage_enter_a_pattern); 
			}
			return false;
		}
		resetMessage(this.bundle != null);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.internal.use.UsePatternPage#resetMessage(boolean)
	 */
	protected void resetMessage(boolean isediting) {
		setErrorMessage(null);
		if(isediting) {
			setMessage(Messages.ArchivePatternPage_edit_acrhive_eclusion_pattern);
		}
		else {
			setMessage(Messages.ArchivePatternPage_create_nested_pattern);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.internal.use.UsePatternPage#getPattern()
	 */
	public String getPattern() {
		return this.bundletext.getText().trim()+':'+this.patterntext.getText().trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.internal.use.UsePatternPage#getKind()
	 */
	public int getKind() {
		return Pattern.JAR;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		return null;
	}
}
