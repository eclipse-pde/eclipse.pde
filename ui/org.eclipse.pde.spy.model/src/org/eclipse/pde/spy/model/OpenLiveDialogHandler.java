/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.spy.model;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.emf.ui.common.IModelResource;
import org.eclipse.e4.tools.emf.ui.internal.wbm.ApplicationModelEditor;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;

@SuppressWarnings("restriction")
public class OpenLiveDialogHandler {
	private Shell shell;

	public OpenLiveDialogHandler() {
	}

	@Execute
	public void run(@Named(IServiceConstants.ACTIVE_SHELL) Shell s, MApplication application, IStylingEngine engine) {
		if (this.shell == null || this.shell.isDisposed()) {
			try {
				this.shell = new Shell(s, SWT.SHELL_TRIM);
				// FIXME Style
				this.shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				this.shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
				FillLayout layout = new FillLayout();
				layout.marginHeight = 10;
				layout.marginWidth = 10;
				this.shell.setLayout(layout);

				final IEclipseContext childContext = application.getContext().createChild("EditorContext");
				MemoryModelResource resource = new MemoryModelResource(application);
				childContext.set(IModelResource.class, resource);
				childContext.set(Composite.class.getName(), shell);
				childContext.set(Shell.class.getName(), shell);
				ContextInjectionFactory.make(ApplicationModelEditor.class, childContext);

				// new ApplicationModelEditor(shell, childContext, resource,
				// null);
				shell.open();
				Display d = shell.getDisplay();
				while (!shell.isDisposed()) {
					if (!d.readAndDispatch()) {
						d.sleep();
					}
				}
				childContext.dispose();
				// d.update();
				shell = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
