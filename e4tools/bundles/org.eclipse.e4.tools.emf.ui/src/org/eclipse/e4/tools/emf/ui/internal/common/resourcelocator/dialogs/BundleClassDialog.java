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
 * Contributors: Steven Spungin <steven@spungin.tv> - initial API and implementation, Bug 424730, Ongoing Maintenance
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.common.IContributionClassCreator;
import org.eclipse.e4.tools.emf.ui.internal.common.ModelEditor;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.Messages;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
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
 * A dialog for creating, editing, replacing, or removing a bundleclass.
 *
 * @author Steven Spungin
 */
public class BundleClassDialog extends AbstractUriDialog {
	private static final String PREFIX = "bundleclass://"; //$NON-NLS-1$
	private static final Pattern PATTERN = Pattern.compile("bundleclass:/*([^/]+)/((.*)\\.)?([^\\.]+)"); //$NON-NLS-1$

	private Text txtPackage;
	private Text txtClass;

	public BundleClassDialog(Shell parentShell, IEclipseContext context) {
		super(parentShell, context, PATTERN, Messages.BundleClassDialog_bundleClassEditor,
				Messages.BundleClassDialog_uri, Messages.BundleClassDialog_bundle, PREFIX);
	}

	@Override
	protected void createFields(Composite composite, ModifyListener listener) {
		Link lblPackage = new Link(composite, SWT.NONE);
		lblPackage.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblPackage.setText("<A>" + Messages.BundleClassDialog_package + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$

		txtPackage = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtPackage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtPackage.addModifyListener(listener);

		lblPackage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IEclipseContext childCtx = createBundleFindContext();
				childCtx.set("package", getQualifier()); //$NON-NLS-1$
				childCtx.set("mode", "show-packages"); //$NON-NLS-1$ //$NON-NLS-2$
				FindContributionDialog dlg = new FindContributionDialog(childCtx);
				if (dlg.open() == Dialog.OK) {
					Matcher matcher = getPattern().matcher(getUri(dlg));
					if (matcher.matches()) {
						txtPackage.setText(matcher.group(3));
					}
				}
			}
		});

		Link lblClass = new Link(composite, SWT.NONE);
		lblClass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblClass.setText("<A>" + Messages.BundleClassDialog_class + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$

		txtClass = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtClass.addModifyListener(listener);

		lblClass.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IEclipseContext childCtx = createBundleFindContext();
				childCtx.set("package", getQualifier()); //$NON-NLS-1$
				FindContributionDialog dlg = new FindContributionDialog(childCtx);
				if (dlg.open() == Window.OK) {
					getUriText().setText(getUri(dlg));
				}
			}
		});
	}

	@Override
	protected void createToolItems(ToolBar toolBar) {
		ToolItem btnGo = new ToolItem(toolBar, SWT.PUSH);
		btnGo.setText(Messages.BundleClassDialog_create_goto);
		btnGo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IEclipseContext context = getContext();
				try {
					MPart dummyPart = MBasicFactory.INSTANCE.createPart();
					dummyPart.setContributionURI(getUri());
					final IContributionClassCreator c = context.get(ModelEditor.class).getContributionCreator(
							org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl.Literals.PART);
					c.createOpen(dummyPart, context.get(EditingDomain.class), context.get(IProject.class), getShell());
					setUri(dummyPart.getContributionURI());
					okPressed();
				} catch (Exception e2) {
					// TODO: handle exception
				}
			}
		});

		ToolItem btnFind = new ToolItem(toolBar, SWT.PUSH);
		btnFind.setText(Messages.BundleClassDialog_find + "..."); //$NON-NLS-1$
		btnFind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindContributionDialog dialog = new FindContributionDialog(createFindContext());
				if (dialog.open() == Dialog.OK) {
					getUriText().setText(getUri(dialog));
				}
			}
		});

		createRemoveItem(toolBar, null);
	}

	@Override
	protected void setQualifierText(String pakage) {
		txtPackage.setText(pakage);
	}

	@Override
	protected void setNameText(String clazz) {
		txtClass.setText(clazz);
	}

	@Override
	protected String buildUri(String prefix, String bundle) {
		return prefix + bundle + "/" + txtPackage.getText() + "." + txtClass.getText(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected String getUri(FindContributionDialog dlg) {
		return dlg.getBundleclassUri();
	}
}
