/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Ketan Padegaonkar <KetanPadegaonkar@gmail.com> - bug 241912
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActiveFormEditorSection implements ISpySection {

	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (!(part instanceof FormEditor))
			return;
		FormEditor multiEditor = (FormEditor) part;

		Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if (object == null)
			return;

		IFormPage activePage = multiEditor.getActivePageInstance();

		Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
		section.setText(PDERuntimeMessages.ActiveFormEditorSection_Active_Form_Page);

		FormText text = toolkit.createFormText(section, true);

		section.setClient(text);
		TableWrapData td = new TableWrapData();
		td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		section.setLayoutData(td);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>"); //$NON-NLS-1$
		buffer.append(toolkit.createClassSection(text, NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, "editor tab"), new Class[] {activePage.getClass()})); //$NON-NLS-1$
		buffer.append("</form>"); //$NON-NLS-1$

		text.setText(buffer.toString(), true, false);
		text.layout();

	}
}
