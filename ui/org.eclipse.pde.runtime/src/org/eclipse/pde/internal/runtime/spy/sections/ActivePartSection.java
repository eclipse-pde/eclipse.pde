/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Heiko Seeberger - changes for bug 237764
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.sections;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.SpyFormToolkit;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.E4PartWrapper;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.internal.PopupMenuExtender;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBookView;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @since 3.4
 */
public class ActivePartSection implements ISpySection {

	@Override
	@SuppressWarnings("restriction")
	public void build(ScrolledForm form, SpyFormToolkit toolkit, ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) // if we don't have an active workbench, we don't have a valid selection to analyze
			return;

		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part == null)
			return; // (Bug 237764) if no active part let's do nothing ...

		String partType = part instanceof IEditorPart ? "editor" : "view"; //$NON-NLS-1$ //$NON-NLS-2$
		Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR);

		section.setText(NLS.bind(PDERuntimeMessages.SpyDialog_activePart_title, part.getSite().getRegisteredName()));

		FormText text = toolkit.createFormText(section, true);
		section.setClient(text);
		TableWrapData td = new TableWrapData();
		td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		section.setLayoutData(td);

		//toolkit.createImageAction(section, part.getTitleImage());

		// e4?
		MPart mPart = null;
		if (part instanceof E4PartWrapper) {
			EPartService service = part.getSite().getService(EPartService.class);
			mPart = service.findPart(part.getSite().getId());
		}

		StringBuilder buffer = new StringBuilder();
		buffer.append("<form>"); //$NON-NLS-1$

		// time to analyze the active part
		if (mPart == null || mPart.getObject() == null) {
			buffer.append(toolkit.createClassSection(text,
					NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, partType), part.getClass()));
		} else {
			buffer.append(toolkit.createClassSection(text,
					NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, partType),
					new Class<?>[] { part.getClass(), mPart.getObject().getClass() }));
		}

		if (part instanceof PageBookView) {
			PageBookView outline = (PageBookView) part;
			IPage currentPage = outline.getCurrentPage();
			if (currentPage != null) {
				buffer.append(toolkit.createClassSection(text, PDERuntimeMessages.SpyDialog_activePageBook_title,
						currentPage.getClass()));
			}
		}

		// Best effort to find the contributing plug-in
		Bundle bundle = null;
		if (mPart == null) {
			bundle = FrameworkUtil.getBundle(part.getClass());
		} else if (mPart.getObject() != null) {
			bundle = FrameworkUtil.getBundle(mPart.getObject().getClass());
		}

		toolkit.generatePluginDetailsText(bundle, part.getSite().getId(), partType, buffer, text);

		// get menu information using reflection
		try {
			PartSite site = (PartSite) part.getSite();
			Class<?> clazz = site.getClass().getSuperclass();
			Field field = clazz.getDeclaredField("menuExtenders"); //$NON-NLS-1$
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<PopupMenuExtender> list = (List<PopupMenuExtender>) field.get(site);
			if (list != null && !list.isEmpty()) {
				Set<String> menuIds = new LinkedHashSet<>();
				for (int i = 0; i < list.size(); i++) {
					PopupMenuExtender extender = list.get(i);
					menuIds.addAll(extender.getMenuIds());
				}
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(PDERuntimeMessages.SpyDialog_activeMenuIds);
				buffer.append("</p>"); //$NON-NLS-1$
				for (Iterator<String> it = menuIds.iterator(); it.hasNext();) {
					buffer.append("<li bindent=\"20\" style=\"image\" value=\"menu\">"); //$NON-NLS-1$
					buffer.append(it.next().toString());
					buffer.append("</li>"); //$NON-NLS-1$
				}
				Image menuImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_MENU_OBJ);
				text.setImage("menu", menuImage); //$NON-NLS-1$
			}
		} catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			PDERuntimePlugin.log(e);
		}

		buffer.append("</form>"); //$NON-NLS-1$

		Image idImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_ID_OBJ);
		text.setImage("id", idImage); //$NON-NLS-1$

		text.setText(buffer.toString(), true, false);
		text.requestLayout();
	}

}
