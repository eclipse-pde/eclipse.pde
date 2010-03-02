/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.importing.BundleImportDescription;
import org.eclipse.pde.internal.core.importing.CvsBundleImportDescription;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.ui.IBundeImportWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Allows specific versions versus HEAD to be imported into the workspace.
 */
public class CVSBundleImportPage extends WizardPage implements IBundeImportWizardPage {

	private BundleImportDescription[] descriptions;
	private Button useHead;
	private TableViewer bundlesViewer;

	private static final String CVS_PAGE_USE_HEAD = "org.eclipse.pde.ui.cvs.import.page.head"; //$NON-NLS-1$

	class CvsLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
		 */
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			if (element instanceof CvsBundleImportDescription) {
				CvsBundleImportDescription description = (CvsBundleImportDescription) element;
				String project = description.getProject();
				String version = description.getTag();
				String host = description.getServer();
				styledString.append(project);
				if (version != null && !useHead.getSelection()) {
					styledString.append(' ');
					styledString.append(version, StyledString.DECORATIONS_STYLER);
				}
				styledString.append(' ');
				styledString.append('[', StyledString.DECORATIONS_STYLER);
				styledString.append(host, StyledString.DECORATIONS_STYLER);
				styledString.append(']', StyledString.DECORATIONS_STYLER);
				return styledString;
			}
			styledString.append(element.toString());
			return styledString;
		}
	}

	/**
	 * Constructs the page.
	 */
	public CVSBundleImportPage() {
		super("cvs", PDEUIMessages.CVSBundleImportPage_0, null); //$NON-NLS-1$
		setDescription(PDEUIMessages.CVSBundleImportPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		Composite group = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_HORIZONTAL);

		Button versions = SWTFactory.createRadioButton(group, PDEUIMessages.CVSBundleImportPage_3);
		useHead = SWTFactory.createRadioButton(group, PDEUIMessages.CVSBundleImportPage_2);
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.refresh(true);
			}
		};
		versions.addSelectionListener(listener);
		useHead.addSelectionListener(listener);

		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		bundlesViewer = new TableViewer(table);
		bundlesViewer.setLabelProvider(new CvsLabelProvider());
		bundlesViewer.setContentProvider(new ArrayContentProvider());
		bundlesViewer.setComparator(new ViewerComparator());
		setControl(comp);
		setPageComplete(true);

		// initialize versions versus HEAD
		IDialogSettings settings = getWizard().getDialogSettings();
		boolean head = false;
		boolean found = false;
		if (settings != null) {
			String string = settings.get(CVS_PAGE_USE_HEAD);
			if (string != null) {
				found = true;
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (!found) {
			for (int i = 0; i < descriptions.length; i++) {
				CvsBundleImportDescription description = (CvsBundleImportDescription) descriptions[i];
				if (description.getTag() != null) {
					head = false;
					break;
				}
			}
		}
		useHead.setSelection(head);
		versions.setSelection(!head);

		// fill viewer
		bundlesViewer.setInput(descriptions);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		setPageComplete(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#finish()
	 */
	public boolean finish() {
		boolean head = false;
		if (getControl() != null) {
			head = useHead.getSelection();
			// store settings
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				settings.put(CVS_PAGE_USE_HEAD, head);
			}
		} else {
			// use whatever was used last time
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (head) {
			// modify tags on bundle import descriptions
			for (int i = 0; i < descriptions.length; i++) {
				CvsBundleImportDescription description = (CvsBundleImportDescription) descriptions[i];
				description.setTag(null);
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#getSelection()
	 */
	public BundleImportDescription[] getSelection() {
		return descriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#setSelection(org.eclipse.pde.core.importing.BundleImportDescription[])
	 */
	public void setSelection(BundleImportDescription[] descriptions) {
		this.descriptions = descriptions;
	}

}
