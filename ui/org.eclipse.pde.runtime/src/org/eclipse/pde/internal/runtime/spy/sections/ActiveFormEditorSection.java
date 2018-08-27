/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Ketan Padegaonkar <KetanPadegaonkar@gmail.com> - bug 241912
 *     Tomasz Zarna <tomasz.zarna@tasktop.com> - bug 299298
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 509400
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActiveFormEditorSection implements ISpySection {

	@Override
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
		if (activePage == null)
			return;

		Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
		section.setText(PDERuntimeMessages.ActiveFormEditorSection_Active_Form_Page);

		FormText text = toolkit.createFormText(section, true);

		section.setClient(text);
		TableWrapData td = new TableWrapData();
		td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		section.setLayoutData(td);

		StringBuilder buffer = new StringBuilder();
		buffer.append("<form>"); //$NON-NLS-1$
		buffer.append(toolkit.createClassSection(text,
				NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, "editor tab"), activePage.getClass())); //$NON-NLS-1$

		SectionPart activeSection = getActiveFormSection(activePage);
		if (activeSection != null) {
			buffer.append(toolkit.createClassSection(text,
					NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, "form section"), //$NON-NLS-1$
					activeSection.getClass()));

		}
		buffer.append("</form>"); //$NON-NLS-1$

		text.setText(buffer.toString(), true, false);
		text.requestLayout();

	}

	/**
	 * Answer form section which can be considered active (has focus).
	 * @return form section or null in case there is no such section.
	 */
	private SectionPart getActiveFormSection(IFormPage activePage) {
		Control focusedControl = Display.getCurrent().getFocusControl();
		if (focusedControl == null) {
			return null;
		}

		IManagedForm form = activePage.getManagedForm();
		if (form == null) {
			return null;
		}

		//find section which controls contains actual focus control (also recursively)
		for (IFormPart formPart : form.getParts()) {
			if (formPart instanceof SectionPart) {
				SectionPart formSection = (SectionPart) formPart;
				Control[] sectionWidgets = formSection.getSection().getChildren();
				for (Control widget : sectionWidgets) {
					if (contains(widget, focusedControl)) {
						return formSection;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Answer if particular control is contained in other control (which can be composite).
	 */
	private boolean contains(Control whereToLook, Control whatToFind) {
		if (whereToLook == null || whatToFind == null) {
			return false;
		}

		if (whereToLook instanceof Composite) {
			Composite compositeWhereToLook = (Composite) whereToLook;

			for (Control child : compositeWhereToLook.getChildren()) {
				if (contains(child, whatToFind)) {
					return true;
				}
			}
		}
		return whereToLook == whatToFind;
	}
}
