/*******************************************************************************
 * Copyright (c) 2017, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.util.Collection;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.TreeContentProvider;
import org.eclipse.pde.internal.ui.shared.target.IEditBundleContainerPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public abstract class BndTargetLocationPage extends WizardPage implements IEditBundleContainerPage {
	private final String			message;
	private final ITargetDefinition	targetDefinition;
	private static final Image bundleIcon = PDEPluginImages.get("/icons/bndtools/bundle.svg"); //$NON-NLS-1$

	public BndTargetLocationPage(String pageName, String title, String message, ITargetDefinition targetDefinition) {
		super(pageName);
		setTitle(title);
		setMessage(message);

		this.message = message;
		this.targetDefinition = targetDefinition;
	}

	public ITargetDefinition getTargetDefinition() {
		return targetDefinition;
	}

	protected void logError(String message, Exception e) {
		ILog.get().error(message, e);
		setMessage(message, IMessageProvider.ERROR);
	}

	protected void logWarning(String message, Exception e) {
		ILog.get().warn(message, e);
		setMessage(message, IMessageProvider.WARNING);
	}

	protected void resetMessage() {
		setMessage(message);
	}

	protected TreeViewer createBundleListArea(Composite composite, int hSpan) {
		TreeViewer bundleList = new TreeViewer(
			new Tree(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER));
		bundleList.getTree()
			.setLayoutData(fillGridData(hSpan));
		bundleList.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return bundleIcon;
			}
		});
		bundleList.setContentProvider(new TreeContentProvider() {
			@Override
			public Object[] getElements(Object element) {
				return ((Collection<?>) element).toArray();
			}
		});
		return bundleList;
	}

	protected Object fillGridData(int hSpan) {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = hSpan;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		return gridData;
	}

	@Override
	public void storeSettings() {}
}
