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
package org.eclipse.pde.internal.ui.shared.target;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page that displays the contents of a bundle container in a table
 *
 */
public class PreviewContainerPage extends WizardPage {

	private ITargetDefinition fTarget;
	private EditDirectoryContainerPage fPage1;
	protected TableViewer fPreviewTable;
	protected Object fInput;

	protected PreviewContainerPage(ITargetDefinition definition, EditDirectoryContainerPage page1) {
		super("ContainerPreviewPage"); //$NON-NLS-1$
		setTitle(Messages.PreviewContainerPage_1);
		setMessage(Messages.PreviewContainerPage_2);
		fTarget = definition;
		fPage1 = page1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(composite, Messages.PreviewContainerPage_3, 1);
		fPreviewTable = new TableViewer(composite);
		fPreviewTable.setLabelProvider(new BundleInfoLabelProvider(true));
		fPreviewTable.setContentProvider(ArrayContentProvider.getInstance());
		fPreviewTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);
	}

	/**
	 * Refreshes the contents of the preview table, possible resolving the container
	 * @param resolve whether the current container should be resolved if it hasn't been already
	 */
	protected void setInput(final IBundleContainer container) {
		if (container == null) {
			fInput = null;
			fPreviewTable.setInput(null);
			return;
		}

		if (container.isResolved()) {
			fInput = container.getAllBundles();
			fPreviewTable.setInput(fInput);
			return;
		}

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					IStatus result = container.resolve(fTarget, monitor);
					if (monitor.isCanceled()) {
						fInput = new Object[] {Messages.PreviewContainerPage_0};
						throw new InterruptedException();
					} else if (!result.isOK() && !result.isMultiStatus()) {
						fInput = new Object[] {result};
					} else {
						fInput = container.getAllBundles();
					}

				}
			});
			fPreviewTable.setInput(fInput);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
			setErrorMessage(e.getMessage());
		} catch (InterruptedException e) {
			IWizardPage page = getPreviousPage();
			setVisible(false);
			getContainer().showPage(page);
			page.setVisible(true);

		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setInput(fPage1.getBundleContainer());
		}
	}

}
