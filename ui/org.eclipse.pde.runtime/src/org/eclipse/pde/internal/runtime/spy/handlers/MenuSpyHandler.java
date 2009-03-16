/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
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

/**
 * @since 3.5
 */
public class MenuSpyHandler extends AbstractHandler implements Listener {

	private PopupDialog INSTANCE = null;
	private Cursor defaultCursor;
	private Cursor spyCursor;

	public MenuSpyHandler() {
		// do nothing
	}

	public Object execute(ExecutionEvent event) {
		if (event != null) {
			if (INSTANCE != null && INSTANCE.getShell() != null && !INSTANCE.getShell().isDisposed()) {
				INSTANCE.close();
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
		}
		return null;
	}

	// TODO clean up this code
	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.KeyDown :
				if (event.keyCode == SWT.ESC)
					break;
			case SWT.Show :
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
			MenuSpyDialog dialog = new MenuSpyDialog(shell, event, shell.getDisplay().getCursorLocation());
			INSTANCE = dialog;
			dialog.create();
			dialog.open();
			event.doit = false;
			event.type = SWT.None;
		}
	}
}
