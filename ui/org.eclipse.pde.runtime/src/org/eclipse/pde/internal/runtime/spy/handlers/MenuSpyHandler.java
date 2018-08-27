/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     Mickael Istria (Red Hat Inc.) - 434317
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 482175
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.dialogs.MenuSpyDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;

public class MenuSpyHandler extends AbstractHandler implements Listener {

	private PopupDialog popupDialog = null;
	private Cursor defaultCursor;
	private Cursor spyCursor;

	@Override
	public Object execute(ExecutionEvent event) {
		if (popupDialog != null && popupDialog.getShell() != null && !popupDialog.getShell().isDisposed()) {
			popupDialog.close();
		}

		Shell shell = HandlerUtil.getActiveShell(event);
		if (shell != null) {
			Display display = shell.getDisplay();
			display.addFilter(SWT.Selection, this);
			display.addFilter(SWT.KeyDown, this);
			display.addFilter(SWT.Show, this);
			if (display.getActiveShell() != null) {
				defaultCursor = display.getActiveShell().getCursor();
				Image image = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_MENUSPY_OBJ);
				spyCursor = new Cursor(display, image.getImageData(), 7, 7);
				display.getActiveShell().setCursor(spyCursor);
			}
		}
		return null;
	}

	// TODO clean up this code
	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
		case SWT.KeyDown:
			if (event.keyCode == SWT.ESC)
				break;
		case SWT.Show:
			if (spyCursor != null) {
				Shell shell = event.display.getActiveShell();
				if (shell != null) {
					shell.setCursor(spyCursor);
				}
			}
			return;
		}
		event.display.removeFilter(SWT.Selection, this);
		event.display.removeFilter(SWT.KeyDown, this);
		event.display.removeFilter(SWT.Show, this);
		if (spyCursor != null) {
			if (event.display.getActiveShell() != null) {
				event.display.getActiveShell().setCursor(defaultCursor);
				defaultCursor = null;
				spyCursor.dispose();
				spyCursor = null;
			}
		}

		if (event.type == SWT.Selection) {
			Shell shell = event.display.getActiveShell();
			if (shell == null) { // see bug 434317
				if (event.widget instanceof Menu) {
					shell = ((Menu) event.widget).getShell();
				} else if (event.widget instanceof MenuItem) {
					shell = ((MenuItem) event.widget).getParent().getShell();
				}
			}
			MenuSpyDialog dialog = new MenuSpyDialog(shell, event, shell.getDisplay().getCursorLocation());
			popupDialog = dialog;
			dialog.create();
			dialog.open();
			event.doit = false;
			event.type = SWT.None;
		}
	}
}
