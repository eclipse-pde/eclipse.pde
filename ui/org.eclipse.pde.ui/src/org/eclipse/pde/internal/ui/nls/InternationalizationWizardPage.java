/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.nls;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public abstract class InternationalizationWizardPage extends WizardPage {

	public InternationalizationWizardPage(String pageName) {
		super(pageName);
	}

	public InternationalizationWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected Group createFilterContainer(Composite parent, String title, String label) {
		Group container = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 6;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		container.setLayoutData(gd);
		container.setText(title);

		Label templateLabel = new Label(container, SWT.NONE);
		templateLabel.setText(label);
		return container;
	}

	protected Text createFilterText(Composite parent, String initial) {
		Text text = new Text(parent, SWT.BORDER);
		text.setText(initial);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(gd);
		return text;
	}

	protected static <T> List<T> getModels(TableViewer viewer, Class<T> clazz) {
		TableItem[] items = viewer.getTable().getItems();
		return Arrays.stream(items).map(TableItem::getData).map(clazz::cast).collect(Collectors.toList());
	}

	protected static <T> Iterator<T> getSelectedModels(TableViewer viewer, Class<T> clazz) {
		return Arrays.stream(viewer.getStructuredSelection().toArray()).map(clazz::cast).iterator();
	}
}
