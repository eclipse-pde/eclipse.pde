/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Remy Suen <remy.suen@gmail.com> - bug 203451
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
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

/**
 * @since 3.4
 */
public class ActiveHelpSection implements ISpySection {

	private SpyFormToolkit toolkit;

	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		this.toolkit = toolkit;
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if (object == null)
			return;

		StringBuffer helpBuffer = new StringBuffer();
		// process help
		// TODO we need to make this cleaner... help processing is complicated atm
		if (object instanceof PreferenceDialog) {
			PreferenceDialog dialog = (PreferenceDialog) object;
			IPreferencePage page = (IPreferencePage) dialog.getSelectedPage();
			processHelp(page.getControl().getShell(), helpBuffer);
			processChildren(page.getControl(), helpBuffer);
		} else if (object instanceof Dialog) {
			Dialog dialog = (Dialog) object;
			processChildren(dialog.getShell(), helpBuffer);
		} else {
			helpBuffer.append(processControlHelp(event, toolkit));
		}

		if (helpBuffer != null && helpBuffer.length() > 0) {
			Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
			section.setText(PDERuntimeMessages.SpyDialog_activeHelpSection_title);
			section.clientVerticalSpacing = 9;

			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);
			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);

			Image image = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CONTEXTID_OBJ);
			text.setImage("contextid", image); //$NON-NLS-1$

			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append(PDERuntimeMessages.SpyDialog_activeHelpSection_desc);
			buffer.append("</p>"); //$NON-NLS-1$
			buffer.append(helpBuffer.toString());
			buffer.append("</form>"); //$NON-NLS-1$
			String content = buffer.toString().replaceAll("&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
			text.setText(content, true, false);
		}

	}

	private void processHelp(Widget widget, StringBuffer buffer) {
		buffer.append(toolkit.createHelpIdentifierSection(widget));
	}

	private void processChildren(Control control, StringBuffer buffer) {
		processHelp(control, buffer);
		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			Control[] controls = composite.getChildren();
			for (int i = 0; i < controls.length; i++) {
				processChildren(controls[i], buffer);
			}
		}
	}

	private String processControlHelp(ExecutionEvent event, SpyFormToolkit toolkit) {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part == null)
			return null;

		IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
		if (window == null)
			return null;

		StringBuffer buffer = new StringBuffer();

		Shell shell = null;
		Control control = null;

		if (part instanceof IEditorPart) {
			IEditorPart editorPart = (IEditorPart) part;
			shell = editorPart.getSite().getShell();

			for (int j = 0; j < window.getActivePage().getEditorReferences().length; j++) {
				IEditorReference er = window.getActivePage().getEditorReferences()[j];
				if (er.getId().equals(editorPart.getEditorSite().getId()))
					if (er instanceof WorkbenchPartReference) {
						WorkbenchPartReference wpr = (WorkbenchPartReference) er;
						control = wpr.getPane().getControl();
						shell = null;
						break;
					}
			}
		} else if (part instanceof ViewPart) {
			ViewPart viewPart = (ViewPart) part;
			shell = viewPart.getSite().getShell();
			for (int j = 0; j < window.getActivePage().getViewReferences().length; j++) {
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
			buffer.append(toolkit.createHelpIdentifierSection(shell));
			for (int i = 0; i < shell.getChildren().length; i++) {
				processChildren(shell.getChildren()[i], buffer);
			}
		} else if (control != null) {
			// if we don't have org.eclipse.help, we will have problems when trying to load IContextProvider
			if (!PDERuntimePlugin.HAS_IDE_BUNDLES)
				processChildren(control, buffer);
			else {
				IContextProvider provider = (IContextProvider) part.getAdapter(IContextProvider.class);
				IContext context = (provider != null) ? provider.getContext(control) : null;
				if (context != null) {
					buffer.append(toolkit.createHelpIdentifierSection(context));
				} else {
					buffer.append(toolkit.createHelpIdentifierSection(control));
				}
				if (control instanceof Composite) {
					Composite parent = (Composite) control;
					for (int i = 0; i < parent.getChildren().length; i++) {
						processChildren(parent.getChildren()[i], buffer);
					}
				}
			}
		}
		return buffer.toString();
	}

}
