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
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.SpyBuilder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

public class ActiveWizardSection implements ISpySection {

	public void build(ScrolledForm form, SpyBuilder builder,
			ExecutionEvent event) {
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if(object == null)
			return;
		Class clazz = object.getClass();

		if(object instanceof WizardDialog) {
			WizardDialog dialog = (WizardDialog) object;
			IWizardPage page = dialog.getCurrentPage();
			IWizard wizard = page.getWizard();
			clazz = wizard.getClass();

			FormToolkit toolkit = builder.getFormToolkit();
			Section section = toolkit.createSection(form.getBody(),
					ExpandableComposite.TITLE_BAR);

			// the active wizard
			FormText text = toolkit.createFormText(section, true);
			section.setClient(text);
			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			td.grabHorizontal = true;
			section.setLayoutData(td);

			StringBuffer buffer = new StringBuffer();
			buffer.append("<form>"); //$NON-NLS-1$
			section.setText(NLS.bind(PDERuntimeMessages.SpyDialog_activeWizard_title, wizard.getWindowTitle()));			

			buffer.append(builder.generateClassString(
					PDERuntimeMessages.SpyDialog_activeWizard_desc,
					clazz));				

			PackageAdmin admin = 
				PDERuntimePlugin.getDefault().getPackageAdmin();
			Bundle bundle = admin.getBundle(clazz);
			builder.generatePluginDetailsText(bundle, null, "wizard", buffer, text); //$NON-NLS-1$

			buffer.append(builder.generateClassString(
					PDERuntimeMessages.SpyDialog_activeWizardPage_desc, 
					page.getClass()));
			buffer.append("</form>"); //$NON-NLS-1$
			

			Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
			text.setImage("class", classImage); //$NON-NLS-1$
			if (PDERuntimePlugin.HAS_IDE_BUNDLES) {
				text.addHyperlinkListener(builder.createHyperlinkAdapter());
			}
			text.setText(buffer.toString(), true, false);
		}
	}

}
