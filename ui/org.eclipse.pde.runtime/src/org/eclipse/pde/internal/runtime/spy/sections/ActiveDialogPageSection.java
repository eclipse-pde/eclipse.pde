/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Kevin Doyle <kjdoyle@ca.ibm.com> - bug 207868, 207904
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @since 3.4
 */
public class ActiveDialogPageSection implements ISpySection {

	@Override
	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if (object == null)
			return;
		Class<?> clazz = object.getClass();

		if (object instanceof IPageChangeProvider) {
			IPageChangeProvider pageChangeProvider = (IPageChangeProvider) object;
			Object selectedPage = pageChangeProvider.getSelectedPage();
			if (selectedPage != null) {
				Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);
				section.clientVerticalSpacing = 9;
				if (selectedPage instanceof IDialogPage) {
					IDialogPage page = (IDialogPage) selectedPage;
					clazz = page.getClass();
					section.setText(NLS.bind(PDERuntimeMessages.SpyDialog_activeDialogPageSection_title, page.getTitle()));

				} else {
					clazz = selectedPage.getClass();
					section.setText(PDERuntimeMessages.SpyDialog_activeDialogPageSection_title2);
				}
				// the active page
				FormText text = toolkit.createFormText(section, true);
				section.setClient(text);
				TableWrapData td = new TableWrapData();
				td.align = TableWrapData.FILL;
				td.grabHorizontal = true;
				section.setLayoutData(td);

				StringBuilder buffer = new StringBuilder();
				buffer.append("<form>"); //$NON-NLS-1$

				buffer.append(toolkit.createClassSection(text,
						PDERuntimeMessages.SpyDialog_activeDialogPageSection_desc, clazz));

				Bundle bundle = FrameworkUtil.getBundle(clazz);
				toolkit.generatePluginDetailsText(bundle, null, "dialog page", buffer, text); //$NON-NLS-1$

				buffer.append("</form>"); //$NON-NLS-1$
				text.setText(buffer.toString(), true, false);
			}
		}
	}

}
