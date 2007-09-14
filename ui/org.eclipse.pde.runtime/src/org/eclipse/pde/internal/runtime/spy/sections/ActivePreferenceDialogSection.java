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
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

public class ActivePreferenceDialogSection implements ISpySection {

	public void build(ScrolledForm form, SpyFormToolkit toolkit,
			ExecutionEvent event) {
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if(object == null)
			return;
		Class clazz = object.getClass();

		if(object instanceof PreferenceDialog) {
			PreferenceDialog dialog = (PreferenceDialog) object;
			IPreferencePage page = (IPreferencePage) dialog.getSelectedPage();
			clazz = page.getClass();

			Section section = toolkit.createSection(form.getBody(),
					ExpandableComposite.TITLE_BAR);
			section.setText(NLS.bind(
					PDERuntimeMessages.ActivePreferenceDialogSection_title, 
					page.getTitle()));
			
			// the active page
			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);
			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);

			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$

			buffer.append(toolkit.createClassSection(
					text,
					PDERuntimeMessages.ActivePreferenceDialogSection_desc,
					new Class[] { clazz }));				

			PackageAdmin admin = 
				PDERuntimePlugin.getDefault().getPackageAdmin();
			Bundle bundle = admin.getBundle(clazz);
			toolkit.generatePluginDetailsText(bundle, null, "preference", buffer, text); //$NON-NLS-1$

			buffer.append("</form>"); //$NON-NLS-1$
			text.setText(buffer.toString(), true, false);
		}
	}

}
