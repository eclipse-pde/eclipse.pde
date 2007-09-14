/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class ActiveHelpSection implements ISpySection {

	private static String HELP_KEY = "org.eclipse.ui.help"; //$NON-NLS-1$

	public void build(ScrolledForm form, SpyFormToolkit toolkit,
			ExecutionEvent event) {
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if(object == null)
			return;
		
		StringBuffer helpBuffer = new StringBuffer();
		// process help
		helpBuffer.append(processControlHelp(event, toolkit));
		if(object instanceof IDialogPage) {
			IDialogPage page = (IDialogPage) object;
			processChildControlHelp(page.getControl().getShell(), toolkit, helpBuffer);
		} else if(object instanceof Dialog) {
			Dialog dialog = (Dialog) object;
			processChildControlHelp(dialog.getShell(), toolkit, helpBuffer);
		}
		
		// ensure we actually have help
		// TODO we need to make this cleaner... help processing is complicated atm
		if(helpBuffer != null && helpBuffer.length() > 0) {
			Section section = toolkit.createSection(form.getBody(),
					ExpandableComposite.TITLE_BAR);
			section.setText("Active Help"); //$NON-NLS-1$
			
			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);
			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);
			
			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$
			buffer.append(helpBuffer.toString());
			buffer.append("</form>"); //$NON-NLS-1$
			text.setText(buffer.toString(), true, false);
		}
		
	}

	private void processChildControlHelp(Control control, SpyFormToolkit toolkit, StringBuffer buffer) {
		if(control.getData(HELP_KEY) != null) {
			buffer.append("<li bindent=\"20\">" + control.getData(HELP_KEY) + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(control instanceof Composite) {
			Composite parent = (Composite) control;
			for (int i = 0; i < parent.getChildren().length; i++) {
				processChildControlHelp(parent.getChildren()[i], toolkit, buffer);
			}
		}
		else if(control instanceof Shell) {
			for(int i = 0; i < ((Shell)control).getChildren().length; i++) {
				processChildControlHelp(((Shell)control).getChildren()[i], toolkit, buffer);
			}
		}
	}

	private String processControlHelp(ExecutionEvent event, SpyFormToolkit toolkit) {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if(part == null)
			return null;
		
		IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
		if(window == null)
			return null;
		
		StringBuffer buffer = new StringBuffer();

		Shell shell = null;
		Control control = null;

		if(part instanceof IEditorPart) {
			IEditorPart editorPart = (IEditorPart) part;
			shell = editorPart.getSite().getShell();

			for(int j = 0; j < window.getActivePage().getEditorReferences().length; j++) {
				IEditorReference er = window.getActivePage().getEditorReferences()[j];
				if (er.getId().equals(editorPart.getEditorSite().getId()))
					if (er instanceof WorkbenchPartReference) {
						WorkbenchPartReference wpr = (WorkbenchPartReference) er;
						control = wpr.getPane().getControl();
						shell = null;
						break;
					}
			}
		}
		else if(part instanceof ViewPart) {
			ViewPart viewPart = (ViewPart) part;
			shell = viewPart.getSite().getShell();
			for(int j = 0; j < window.getActivePage().getViewReferences().length; j++) {
				IViewReference vr = window.getActivePage().getViewReferences()[j];
				if (vr.getId().equals(viewPart.getViewSite().getId()))
					if (vr instanceof WorkbenchPartReference) {
						WorkbenchPartReference wpr = (WorkbenchPartReference) vr;
						control = wpr.getPane().getControl();
						shell = null;
						break;
					}
			}

		}
		if (shell != null) {
			buffer.append("<p>Help IDs:</p>"); //$NON-NLS-1$
			if (shell.getData(HELP_KEY) != null) { 
				buffer.append("<li bindent=\"20\">" + shell.getData(HELP_KEY) + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (int i = 0; i < shell.getChildren().length; i++) {
				processChildControlHelp(shell.getChildren()[i], toolkit, buffer);
			}
		}
		else if(control != null) {
			if(control.getData(HELP_KEY) != null) { 
				buffer.append("<p>Help Data:</p>"); //$NON-NLS-1$
				buffer.append("<li bindent=\"20\">" + shell.getData(HELP_KEY) + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if(control instanceof Composite) {
				Composite parent = (Composite) control;
				for(int i = 0; i < parent.getChildren().length; i++) {
					processChildControlHelp(parent.getChildren()[i], toolkit, buffer);
				}
			}
		}
		return buffer.toString();
	}
	
}
