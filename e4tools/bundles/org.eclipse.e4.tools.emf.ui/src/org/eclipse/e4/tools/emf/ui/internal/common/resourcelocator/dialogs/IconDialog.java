/*******************************************************************************
 * Copyright (c) 2014 TwelveTone LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Steven Spungin <steven@spungin.tv> - initial API and implementation, Bug 424730
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.util.regex.Pattern;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A dialog for creating, editing, replacing, or removing an icon URI.
 *
 * @author Steven Spungin
 */
public class IconDialog extends AbstractUriDialog {
	private static final String PREFIX = "platform:/plugin/"; //$NON-NLS-1$
	private static final Pattern PATTERN = Pattern.compile("platform:/plugin/*([^/]+)/((.*)/)?([^/]+)"); //$NON-NLS-1$

	private Text txtPath;
	private Text txtIcon;

	public IconDialog(Shell parentShell, IEclipseContext context) {
		super(parentShell, context, PATTERN, Messages.IconDialog_uriEditor, Messages.IconDialog_uri,
				Messages.IconDialog_bundle, PREFIX);
	}

	@Override
	protected void createFields(Composite composite, ModifyListener listener) {
		Link lblPath = new Link(composite, SWT.NONE);
		lblPath.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblPath.setText("path"); //$NON-NLS-1$

		txtPath = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtPath.addModifyListener(listener);

		Link lblIcon = new Link(composite, SWT.NONE);
		lblIcon.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblIcon.setText("<A>" + Messages.IconDialog_icon + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$

		txtIcon = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtIcon.addModifyListener(listener);

		lblIcon.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindIconDialog dlg = new FindIconDialog(getParentShell(), getContext().createChild(), getBundle());
				if (dlg.open() == Dialog.OK) {
					getUriText().setText(dlg.getValue());
				}
			}
		});
	}

	@Override
	protected void createToolItems(ToolBar toolBar) {
		ResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), toolBar);

		ToolItem btnFind = new ToolItem(toolBar, SWT.PUSH);
		btnFind.setText(Messages.IconDialog_find + "..."); //$NON-NLS-1$
		btnFind.setImage(
				resources.create(ImageDescriptor.createFromFile(IconDialog.class, "/icons/full/obj16/find.png"))); //$NON-NLS-1$
		btnFind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindIconDialog dlg = new FindIconDialog(getParentShell(), getContext().createChild());
				if (dlg.open() == Dialog.OK) {
					getUriText().setText(dlg.getValue());
					getShell().pack();
				}
			}
		});

		createRemoveItem(toolBar, resources.create(
				ImageDescriptor.createFromFile(IconDialog.class, "/icons/full/obj16/remove_filter.png"))); //$NON-NLS-1$
	}

	@Override
	protected void setQualifierText(String path) {
		txtPath.setText(path);
	}

	@Override
	protected void setNameText(String icon) {
		txtIcon.setText(icon);
	}

	@Override
	protected String buildUri(String prefix, String bundle) {
		return prefix + bundle + "/" + txtPath.getText() + "/" + txtIcon.getText(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected String getUri(FindContributionDialog dlg) {
		return dlg.getPlatformUri();
	}
}
