/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.api.tools.ui.internal.use.ApiUsePatternTab.Pattern;

/**
 * Wizard for creating patterns
 * 
 * @since 1.0.1
 */
public class PatternWizard extends Wizard {
	
	private String pattern = null;
	private int kind = -1;

	/**
	 * Constructor
	 * @param pattern
	 * @param kind
	 */
	public PatternWizard(String pattern, int kind) {
		setWindowTitle(Messages.PatternWizard_use_scan_patterns);
		this.pattern = pattern;
		this.kind = kind;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		if(this.pattern == null) {
			addPage(new PatternSelectionPage());
			addPage(new DescriptionPatternPage(null, -1));
			addPage(new ArchivePatternPage(null));
			addPage(new ReportPatternPage(null, -1));
		}
		else {
			switch(this.kind) {
				case Pattern.API:
				case Pattern.INTERNAL: {
					addPage(new DescriptionPatternPage(this.pattern, this.kind));
					break;
				}
				case Pattern.JAR: {
					addPage(new ArchivePatternPage(this.pattern));
					break;
				}
				case Pattern.REPORT_TO:
				case Pattern.REPORT: {
					addPage(new ReportPatternPage(this.pattern, this.kind));
					break;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		IWizardPage page = getStartingPage();
		UsePatternPage upage = null;
		if(page instanceof PatternSelectionPage) {
			upage = (UsePatternPage) getPage(((PatternSelectionPage)page).nextPage());
		}
		else {
			upage = (UsePatternPage) page;
		}
		this.pattern = upage.getPattern();
		this.kind = upage.getKind();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		String name = getStartingPage().getName();
		if(!name.equals(PatternSelectionPage.PAGE_NAME)) {
			return getStartingPage().isPageComplete();
		}
		IWizardPage page = getStartingPage().getNextPage();
		if(page != null) {
			return page.isPageComplete();
		}
		return false;
	}
	
	/**
	 * @return the pattern
	 */
	public String getPattern() {
		return this.pattern;
	}
	
	/**
	 * @return the pattern kind
	 */
	public int getKind() {
		return this.kind;
	}
}
