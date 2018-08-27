/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.shared.target;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

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

	@Override
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(composite, Messages.PreviewContainerPage_3, 1);
		fPreviewTable = new TableViewer(composite);
		fPreviewTable.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fPreviewTable.setContentProvider(ArrayContentProvider.getInstance());
		fPreviewTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LOCATION_PREVIEW_WIZARD);
	}

	/**
	 * Refreshes the contents of the preview table, possible resolving the container
	 */
	protected void setInput(final ITargetLocation container) {
		if (container == null) {
			fInput = null;
			fPreviewTable.setInput(null);
			return;
		}

		if (container.isResolved()) {
			fInput = container.getBundles();
			fPreviewTable.setInput(fInput);
			return;
		}

		try {
			getContainer().run(true, true, monitor -> {
				IStatus result = container.resolve(fTarget, monitor);
				if (monitor.isCanceled()) {
					fInput = new Object[] {Messages.PreviewContainerPage_0};
				} else if (!result.isOK() && !result.isMultiStatus()) {
					fInput = new Object[] {result};
				} else {
					fInput = container.getBundles();
				}

			});
			fPreviewTable.setInput(fInput);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
			setErrorMessage(e.getMessage());
		} catch (InterruptedException e) {
		}

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setInput(fPage1.getBundleContainer());
		}
	}

}
