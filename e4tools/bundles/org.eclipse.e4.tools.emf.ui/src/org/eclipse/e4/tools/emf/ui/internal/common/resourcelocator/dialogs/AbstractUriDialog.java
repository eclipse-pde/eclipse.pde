/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * A dialog that edits a URI of the form {@code scheme/bundle/qualifier/name} through a text field per part. The
 * subclass supplies the URI pattern, the fields for the qualifier and the name, and the tool bar items.
 *
 * @author Steven Spungin
 */
public abstract class AbstractUriDialog extends Dialog implements UriDialog {
	private final IEclipseContext context;
	private final Pattern pattern;
	private final String shellTitle;
	private final String uriLabel;
	private final String bundleLabel;
	private final String uriPrefix;
	private Text txtUri;
	private Text txtBundle;
	private String uri = ""; //$NON-NLS-1$
	private String bundle = ""; //$NON-NLS-1$
	private String qualifier = ""; //$NON-NLS-1$
	private String name = ""; //$NON-NLS-1$
	private boolean ignoreModify;

	/**
	 * @param pattern
	 *            matches the URI; group 1 is the bundle, group 3 the optional qualifier and group 4 the name
	 * @param uriPrefix
	 *            the part of the URI in front of the bundle
	 */
	protected AbstractUriDialog(Shell parentShell, IEclipseContext context, Pattern pattern, String shellTitle,
			String uriLabel, String bundleLabel, String uriPrefix) {
		super(parentShell);
		this.context = context;
		this.pattern = pattern;
		this.shellTitle = shellTitle;
		this.uriLabel = uriLabel;
		this.bundleLabel = bundleLabel;
		this.uriPrefix = uriPrefix;
	}

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText(shellTitle);
		super.configureShell(newShell);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));

		ToolBar toolBar = new ToolBar(composite, SWT.NO_FOCUS);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label lblUri = new Label(composite, SWT.NONE);
		lblUri.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblUri.setText(uriLabel);

		txtUri = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtUri.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtUri.setText(uriPrefix);
		txtUri.addModifyListener(e -> {
			if (ignoreModify) {
				return;
			}
			ignoreModify = true;
			setUri(txtUri.getText());
			txtBundle.setText(bundle);
			setQualifierText(qualifier);
			setNameText(name);
			ignoreModify = false;
			parent.pack();
		});

		// common listener for the part text fields
		ModifyListener listener = e -> {
			if (ignoreModify) {
				return;
			}
			ignoreModify = true;
			setUri(buildUri(uriPrefix, txtBundle.getText()));
			txtUri.setText(getUri());
			ignoreModify = false;
		};

		Link lblBundle = new Link(composite, SWT.NONE);
		lblBundle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lblBundle.setText("<A>" + bundleLabel + "</A>"); //$NON-NLS-1$ //$NON-NLS-2$

		txtBundle = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		txtBundle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtBundle.addModifyListener(listener);

		lblBundle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IEclipseContext childCtx = createBundleFindContext();
				childCtx.set("package", getQualifier()); //$NON-NLS-1$
				childCtx.set("mode", "show-bundles"); //$NON-NLS-1$ //$NON-NLS-2$
				FindContributionDialog dlg = new FindContributionDialog(childCtx);
				if (dlg.open() == Dialog.OK) {
					Matcher matcher = pattern.matcher(getUri(dlg));
					if (matcher.matches()) {
						txtBundle.setText(matcher.group(1));
					}
				}
			}
		});

		createFields(composite, listener);
		createToolItems(toolBar);

		txtUri.setText(uri);
		txtBundle.setText(bundle);
		setNameText(name);
		return composite;
	}

	/** Creates the fields for the qualifier and the name; modifications must be reported to the listener. */
	protected abstract void createFields(Composite composite, ModifyListener listener);

	protected abstract void createToolItems(ToolBar toolBar);

	protected abstract void setQualifierText(String qualifier);

	protected abstract void setNameText(String name);

	/** Returns the URI built from the prefix, the bundle and the text of the qualifier and name fields. */
	protected abstract String buildUri(String prefix, String bundleText);

	/** Returns the URI the user picked in the dialog. */
	protected abstract String getUri(FindContributionDialog dlg);

	/** Creates a context for a {@link FindContributionDialog}. */
	protected final IEclipseContext createFindContext() {
		IEclipseContext childCtx = context.createChild();
		childCtx.set(Bundle.class, FrameworkUtil.getBundle(FindContributionDialog.class));
		return childCtx;
	}

	/** Creates a find context that is preset with the current bundle. */
	protected final IEclipseContext createBundleFindContext() {
		IEclipseContext childCtx = createFindContext();
		childCtx.set("bundle", getBundle()); //$NON-NLS-1$
		return childCtx;
	}

	protected final IEclipseContext getContext() {
		return context;
	}

	protected final Pattern getPattern() {
		return pattern;
	}

	protected final void createRemoveItem(ToolBar toolBar, Image image) {
		ToolItem btnRemove = new ToolItem(toolBar, SWT.PUSH);
		btnRemove.setText("Remove"); //$NON-NLS-1$
		if (image != null) {
			btnRemove.setImage(image);
		}
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				txtUri.setText(""); //$NON-NLS-1$
				close();
			}
		});
	}

	protected final Text getUriText() {
		return txtUri;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public void setUri(String uri) {
		this.uri = uri == null ? "" : uri; //$NON-NLS-1$
		Matcher matcher = pattern.matcher(this.uri);
		if (matcher.matches()) {
			bundle = matcher.group(1);
			qualifier = matcher.group(3) == null ? "" : matcher.group(3); //$NON-NLS-1$
			name = matcher.group(4);
		} else {
			bundle = ""; //$NON-NLS-1$
			qualifier = ""; //$NON-NLS-1$
			name = ""; //$NON-NLS-1$
		}
	}

	protected final String getBundle() {
		return bundle;
	}

	protected final String getQualifier() {
		return qualifier;
	}

	protected final String getName() {
		return name;
	}
}
