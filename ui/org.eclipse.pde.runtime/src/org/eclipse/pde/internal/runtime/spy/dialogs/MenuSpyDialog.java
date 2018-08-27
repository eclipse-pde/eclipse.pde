/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.dialogs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.pde.internal.runtime.spy.sections.ActiveMenuSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class MenuSpyDialog extends PopupDialog {

	private Event event;
	private Point fAnchor;
	private Composite composite;
	private SpyFormToolkit toolkit;

	private class CloseAction extends Action {
		@Override
		public ImageDescriptor getImageDescriptor() {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE);
		}

		@Override
		public String getToolTipText() {
			return PDERuntimeMessages.SpyDialog_close;
		}

		@Override
		public void run() {
			close();
		}
	}

	public MenuSpyDialog(Shell parent, Event event, Point point) {
		super(parent, SWT.NONE, true, true, false, false, false, null, null);
		this.event = event;
		this.fAnchor = point;
		this.toolkit = new SpyFormToolkit(this);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.SPY_DIALOG);
	}

	@Override
	protected Control createContents(Composite parent) {
		getShell().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		initializeBounds();
		return createDialogArea(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		this.composite = (Composite) super.createDialogArea(parent);

		ScrolledForm form = toolkit.createScrolledForm(composite);
		toolkit.decorateFormHeading(form.getForm());

		// set title and image
		form.setText(PDERuntimeMessages.MenuSpyDialog_title);
		Image image = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_SPY_OBJ);
		form.setImage(image);

		// add a Close button to the toolbar
		form.getToolBarManager().add(new CloseAction());
		form.getToolBarManager().update(true);

		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.topMargin = 10;
		layout.verticalSpacing = 10;
		form.getBody().setLayout(layout);

		// TODO, make this so we use an extension point.
		ActiveMenuSection section = new ActiveMenuSection();
		section.build(form, toolkit, event);

		parent.pack();
		return composite;
	}

	@Override
	protected Point getInitialLocation(Point size) {
		if (fAnchor == null) {
			return super.getInitialLocation(size);
		}
		Point point = fAnchor;
		Rectangle monitor = getShell().getMonitor().getClientArea();
		if (monitor.width < point.x + size.x) {
			point.x = Math.max(0, point.x - size.x);
		}
		if (monitor.height < point.y + size.y) {
			point.y = Math.max(0, point.y - size.y);
		}
		return point;
	}

	@Override
	public boolean close() {
		if (toolkit != null)
			toolkit.dispose();
		toolkit = null;
		return super.close();
	}

	@Override
	protected Control getFocusControl() {
		return this.composite;
	}

}
