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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.SpyBuilder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActiveSelectionSection implements ISpySection {

	public void build(ScrolledForm form, SpyBuilder builder, ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if(window == null) // if we don't have an active workbench, we don't have a valid selection to analyze
			return;

		// analyze the selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object object = ss.getFirstElement();
			if (object != null) { // check for a valid class
				Class clazz = object.getClass();

				FormToolkit toolkit = builder.getFormToolkit();
				Section section = toolkit.createSection(form.getBody(),
						ExpandableComposite.TITLE_BAR);
				section.setExpanded(true);
				section.setText(PDERuntimeMessages.SpyDialog_activeSelection_title);
				FormText text = toolkit.createFormText(section, true);
				section.setClient(text);

				TableWrapData td = new TableWrapData();
				td.align = TableWrapData.FILL;
				td.grabHorizontal = true;
				section.setLayoutData(td);

				// time to analyze the selection
				StringBuffer buffer = new StringBuffer();
				buffer.append("<form>"); //$NON-NLS-1$
				buffer.append(builder.generateClassString(
						PDERuntimeMessages.SpyDialog_activeSelection_desc, 
						clazz));

				Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
				text.setImage("class", classImage); //$NON-NLS-1$

				Class[] interfaces = clazz.getInterfaces();
				if (interfaces.length > 0) {
					buffer.append("<p>"); //$NON-NLS-1$
					buffer.append(PDERuntimeMessages.SpyDialog_activeSelectionInterfaces_desc);
					buffer.append("</p>"); //$NON-NLS-1$
					for (int i = 0; i < interfaces.length; i++) {
						buffer.append(builder.generateInterfaceString(interfaces[i]));
					}
					Image interfaceImage = PDERuntimePluginImages
					.get(PDERuntimePluginImages.IMG_INTERFACE_OBJ);
					text.setImage("interface", interfaceImage); //$NON-NLS-1$
				}

				buffer.append("</form>"); //$NON-NLS-1$
				if (PDERuntimePlugin.HAS_IDE_BUNDLES) {
					text.addHyperlinkListener(builder.createHyperlinkAdapter());
				}
				text.setText(buffer.toString(), true, false);
			}
		}
	}

}
