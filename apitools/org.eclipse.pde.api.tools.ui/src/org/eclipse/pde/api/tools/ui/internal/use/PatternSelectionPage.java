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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page used to select the kind of pattern to create
 *
 * @since 1.0.1
 */
public class PatternSelectionPage extends WizardPage {

	class PatternElement {
		String name = null, desc = null, imgid = null, pname = null;

		public PatternElement(String name, String desc, String imgid, String pname) {
			this.name = name;
			this.desc = desc;
			this.imgid = imgid;
			this.pname = pname;
		}
	}

	class LP extends LabelProvider {
		@Override
		public String getText(Object element) {
			return ((PatternElement) element).name;
		}

		@Override
		public Image getImage(Object element) {
			PatternElement pelement = (PatternElement) element;
			if (pelement.imgid != null) {
				return ApiUIPlugin.getSharedImage(pelement.imgid);
			}
			return null;
		}
	}

	static final String PAGE_NAME = "select"; //$NON-NLS-1$

	final PatternElement[] fgelements = {
			new PatternElement(Messages.PatternSelectionPage_package_pattern, Messages.PatternSelectionPage_package_pattern_desc, null, DescriptionPatternPage.PAGE_NAME),
			new PatternElement(Messages.PatternSelectionPage_archive_pattern, Messages.PatternSelectionPage_archive_pattern_desc, null, ArchivePatternPage.PAGE_NAME),
			new PatternElement(Messages.PatternSelectionPage_report_conversion_pattern, Messages.PatternSelectionPage_report_conversion_pattern_desc, null, ReportPatternPage.PAGE_NAME) };

	TableViewer viewer = null;
	Text description = null;

	/**
	 * Constructor
	 */
	protected PatternSelectionPage() {
		super(PAGE_NAME, Messages.PatternSelectionPage_select_pattern, null);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 2, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(comp, Messages.PatternSelectionPage_pattern_types, 1);
		this.viewer = new TableViewer(new Table(comp, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION));
		this.viewer.setLabelProvider(new LP());
		this.viewer.setContentProvider(ArrayContentProvider.getInstance());
		this.viewer.setInput(fgelements);
		this.viewer.addSelectionChangedListener(event -> {
			String desc = getSelectedElement().desc;
			PatternSelectionPage.this.description
					.setText((desc == null ? Messages.PatternSelectionPage_no_desc : desc));
			setPageComplete(isPageComplete());
		});
		this.viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((PatternElement) e1).name.compareTo(((PatternElement) e2).name);
			}
		});

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 100;
		this.viewer.getTable().setLayoutData(gd);
		SWTFactory.createHorizontalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, Messages.PatternSelectionPage_description, 1);
		this.description = new Text(comp, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
		this.description.setEnabled(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 50;
		this.description.setLayoutData(gd);
		if (fgelements != null && fgelements.length > 0) {
			this.viewer.setSelection(new StructuredSelection(this.viewer.getElementAt(0)), true);
		}
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IApiToolsHelpContextIds.APITOOLS_PATTERN_SELECTION_WIZARD_PAGE);
	}

	@Override
	public boolean isPageComplete() {
		if (this.viewer.getStructuredSelection().isEmpty()) {
			setErrorMessage(Messages.PatternSelectionPage_must_select_type);
			return false;
		}
		setErrorMessage(null);
		setMessage(Messages.PatternSelectionPage_select_type);
		return true;
	}

	/**
	 * @return the selected element in the table
	 */
	PatternElement getSelectedElement() {
		IStructuredSelection ss = this.viewer.getStructuredSelection();
		return (PatternElement) ss.getFirstElement();
	}

	@Override
	public IWizardPage getNextPage() {
		return getWizard().getPage(nextPage());
	}

	/**
	 * @return the id of the next page to show based on the selection on this
	 *         page
	 */
	public String nextPage() {
		PatternElement element = getSelectedElement();
		if (element != null) {
			return element.pname;
		}
		return null;
	}
}
