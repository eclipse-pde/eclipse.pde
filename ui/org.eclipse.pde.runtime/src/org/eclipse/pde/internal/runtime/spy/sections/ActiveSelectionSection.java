/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @since 3.4
 */
public class ActiveSelectionSection implements ISpySection {

	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) // if we don't have an active workbench, we don't have a valid selection to analyze
			return;

		// analyze the selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (selection != null && window.getShell() == shell) {
			Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
			section.clientVerticalSpacing = 9;
			section.setText(PDERuntimeMessages.SpyDialog_activeSelection_title);
			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);

			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);

			// time to analyze the selection
			Class clazz = selection.getClass();
			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$
			buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.SpyDialog_activeSelection_desc, new Class[] {clazz}));

			Class[] interfaces = clazz.getInterfaces();
			buffer.append(toolkit.createInterfaceSection(text, PDERuntimeMessages.SpyDialog_activeSelectionInterfaces_desc, interfaces));

			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				int size = ss.size();
				if (size == 1) {
					clazz = ss.getFirstElement().getClass();
					buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.SpyDialog_activeSelectedElement_desc, new Class[] {clazz}));

					interfaces = clazz.getInterfaces();
					buffer.append(toolkit.createInterfaceSection(text, PDERuntimeMessages.SpyDialog_activeSelectedElementInterfaces_desc, interfaces));
				} else if (size > 1) {
					buffer.append(NLS.bind(PDERuntimeMessages.SpyDialog_activeSelectedElementsCount_desc, new Integer(size)));
				}
			}

			buffer.append("</form>"); //$NON-NLS-1$
			text.setText(buffer.toString(), true, false);
		}
	}

}
