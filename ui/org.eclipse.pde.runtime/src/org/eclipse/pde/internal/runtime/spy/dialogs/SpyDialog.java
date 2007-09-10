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
package org.eclipse.pde.internal.runtime.spy.dialogs;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.SpyIDEUtil;
import org.eclipse.pde.internal.runtime.spy.SpyRCPUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.internal.PopupMenuExtender;
import org.osgi.framework.Bundle;

public class SpyDialog extends PopupDialog {

	private ExecutionEvent event;
	private Point fAnchor;
	private Map bundleByClassName = new HashMap();
	private FormToolkit toolkit;
	private Composite composite;

	public SpyDialog(Shell parent, ExecutionEvent event, Point point) {
		super(parent, SWT.NONE, true, false, false, false, null, null);
		this.event = event;
		this.fAnchor = point;
		toolkit = new FormToolkit(parent.getDisplay());
	}

	protected Control createContents(Composite parent) {
		getShell().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		initializeBounds();
		return createDialogArea(parent);
	}
	
	protected Control createDialogArea(Composite parent) {
		this.composite = (Composite) super.createDialogArea(parent);

		ScrolledForm form = toolkit.createScrolledForm(composite);
		toolkit.decorateFormHeading(form.getForm());

		// set title and image
		form.setText(PDERuntimeMessages.SpyDialog_title);
		Image image = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_SPY_OBJ);
		form.setImage(image);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.topMargin = 10;
		layout.verticalSpacing = 10;
		form.getBody().setLayout(layout);

		// create the sections
		// TODO refactor this out 
		createShellSection(toolkit, form);
		createPartSection(toolkit, form);
		createSelectionSection(toolkit, form);

		parent.pack();
		return composite;
	}

	private void createShellSection(FormToolkit toolkit, ScrolledForm form) {
		final Shell shell = HandlerUtil.getActiveShell(event);
		Object object = shell.getData();
		if(object == null)
			return;
		Class clazz = object.getClass();
		
		Section section = toolkit.createSection(form.getBody(),
				ExpandableComposite.TITLE_BAR);

		section.setText(PDERuntimeMessages.SpyDialog_activeShell_title);

		FormText text = toolkit.createFormText(section, true);
		section.setClient(text);
		TableWrapData td = new TableWrapData();
		td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		section.setLayoutData(td);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>"); //$NON-NLS-1$

		buffer.append(SpyRCPUtil.generateClassString(
				PDERuntimeMessages.SpyDialog_activeShell_desc, 
				bundleByClassName, 
				clazz));
		
		// do some wizard inspection
		// TODO create own section for this
		if(object instanceof WizardDialog) {
			WizardDialog dialog = (WizardDialog) object;
			IWizardPage page = dialog.getCurrentPage();
			IWizard wizard = page.getWizard();
			clazz = wizard.getClass();

			Section wizardSection = toolkit.createSection(form.getBody(),
					ExpandableComposite.TITLE_BAR);

			wizardSection.setText(NLS.bind(PDERuntimeMessages.SpyDialog_activeWizard_title, wizard.getWindowTitle()));			
			
			StringBuffer wizardBuffer = new StringBuffer("<form>"); //$NON-NLS-1$
			wizardBuffer.append(SpyRCPUtil.generateClassString(
					PDERuntimeMessages.SpyDialog_activeWizard_desc, 
					bundleByClassName, 
					clazz));				
			
			FormText wizardText = toolkit.createFormText(wizardSection, true);
			wizardSection.setClient(wizardText);
			TableWrapData td1 = new TableWrapData();
			td1.align = TableWrapData.FILL;
			td1.grabHorizontal = true;
			wizardSection.setLayoutData(td1);
			
			Bundle bundle = (Bundle)bundleByClassName.get(clazz.getName());
			createPluginDetailsText(bundle, null, "wizard", wizardBuffer, wizardText); //$NON-NLS-1$
			
			wizardBuffer.append(SpyRCPUtil.generateClassString(
					PDERuntimeMessages.SpyDialog_activeWizardPage_desc, 
					bundleByClassName, 
					page.getClass()));
			
			wizardBuffer.append("</form>"); //$NON-NLS-1$
			
			Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
			wizardText.setImage("class", classImage); //$NON-NLS-1$
			
			wizardText.setText(wizardBuffer.toString(), true, false);
		}
		
		Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
		text.setImage("class", classImage); //$NON-NLS-1$

		// TODO, create a hyperlink adapter
		if (!bundleByClassName.isEmpty()) {
			text.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					String href = (String) e.getHref();
					SpyIDEUtil.openClass(((Bundle) bundleByClassName
							.get(href)).getSymbolicName(), href);
					close();
				}

			});
		}

		buffer.append("</form>"); //$NON-NLS-1$
		text.setText(buffer.toString(), true, false);
	}

	private void createPartSection(FormToolkit toolkit, ScrolledForm form) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if(window == null) // if we don't have an active workbench, we don't have a valid selection to analyze
			return;
		
		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		String partType = part instanceof IEditorPart ? "editor" : "view"; //$NON-NLS-1$ //$NON-NLS-2$
		Section section = toolkit.createSection(form.getBody(),
				ExpandableComposite.TITLE_BAR);

		section.setText(NLS.bind(
				PDERuntimeMessages.SpyDialog_activePart_title, 
				part.getSite().getRegisteredName()));

		FormText text = toolkit.createFormText(section, true);
		section.setClient(text);
		TableWrapData td = new TableWrapData();
		td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		section.setLayoutData(td);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>"); //$NON-NLS-1$

		// time to analyze the active part
		buffer.append(SpyRCPUtil.generateClassString(NLS.bind(PDERuntimeMessages.SpyDialog_activePart_desc, partType),
				bundleByClassName, 
				part.getClass()));

		// time to analyze the contributing plug-in
		final Bundle bundle = Platform.getBundle(part.getSite().getPluginId());

		createPluginDetailsText(bundle, part.getSite().getId(), partType, buffer, text);

		// get menu information using reflection
		try {
			PartSite site = (PartSite) part.getSite();
			Class clazz = site.getClass().getSuperclass();
			Field field = clazz.getDeclaredField("menuExtenders"); //$NON-NLS-1$
			field.setAccessible(true);
			List list = (List) field.get(site);
			if (list != null && list.size() > 0) {
				Set menuIds = new LinkedHashSet();
				for (int i = 0; i < list.size(); i++) {
					PopupMenuExtender extender = (PopupMenuExtender) list
							.get(i);
					menuIds.addAll(extender.getMenuIds());
				}
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(PDERuntimeMessages.SpyDialog_activeMenuIds);
				buffer.append("</p>"); //$NON-NLS-1$
				for (Iterator it = menuIds.iterator(); it.hasNext();) {
					buffer.append("<li bindent=\"20\" style=\"image\" value=\"menu\">"); //$NON-NLS-1$
					buffer.append(it.next().toString());
					buffer.append("</li>"); //$NON-NLS-1$
				}
				Image menuImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_MENU_OBJ);
				text.setImage("menu", menuImage); //$NON-NLS-1$
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		buffer.append("</form>"); //$NON-NLS-1$

		Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
		text.setImage("class", classImage); //$NON-NLS-1$

		Image idImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_ID_OBJ);
		text.setImage("id", idImage); //$NON-NLS-1$

		if (PDERuntimePlugin.HAS_IDE_BUNDLES) {
			text.addHyperlinkListener(new HyperlinkAdapter() {

				public void linkActivated(HyperlinkEvent e) {
					String href = (String) e.getHref();
					SpyIDEUtil.openClass(bundle.getSymbolicName(), href);
					close();
				}

			});
		}

		text.setText(buffer.toString(), true, false);
	}
	
	private void createPluginDetailsText(Bundle bundle, String objectId, String objectType, StringBuffer buffer, FormText text) {
		if (bundle != null) {
			String version = (String) (bundle.getHeaders()
					.get(org.osgi.framework.Constants.BUNDLE_VERSION));
			
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append(PDERuntimeMessages.SpyDialog_contributingPluginId_title);
			buffer.append("</p>"); //$NON-NLS-1$
			buffer.append("<li bindent=\"20\" style=\"image\" value=\"plugin\">"); //$NON-NLS-1$
			buffer.append(bundle.getSymbolicName());
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(version);
			buffer.append(")"); //$NON-NLS-1$
			buffer.append("</li>"); //$NON-NLS-1$
	
			Image pluginImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_PLUGIN_OBJ);
			text.setImage("plugin", pluginImage); //$NON-NLS-1$
	
			if (objectId != null) {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(NLS.bind(PDERuntimeMessages.SpyDialog_contributingPluginId_desc, objectType));
				buffer.append("</p>"); //$NON-NLS-1$
				buffer.append("<li bindent=\"20\" style=\"image\" value=\"id\">"); //$NON-NLS-1$
				buffer.append(objectId);
				buffer.append("</li>"); //$NON-NLS-1$
			}
		}
	}

	private void createSelectionSection(FormToolkit toolkit, ScrolledForm form) {
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if(window == null) // if we don't have an active workbench, we don't have a valid selection to analyze
			return;
		
		// analyze the selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object object = ss.getFirstElement();
			if (object != null) { // check for a valid class
				final Map bundleByClassName = new HashMap();
				Class clazz = object.getClass();

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
				buffer.append(SpyRCPUtil.generateClassString(
						PDERuntimeMessages.SpyDialog_activeSelection_desc, 
						bundleByClassName, 
						clazz));

				Image classImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
				text.setImage("class", classImage); //$NON-NLS-1$

				Class[] interfaces = clazz.getInterfaces();
				if (interfaces.length > 0) {
					buffer.append("<p>"); //$NON-NLS-1$
					buffer.append(PDERuntimeMessages.SpyDialog_activeSelectionInterfaces_desc);
					buffer.append("</p>"); //$NON-NLS-1$
					for (int i = 0; i < interfaces.length; i++) {
						buffer.append(SpyRCPUtil.generateInterfaceString(bundleByClassName, interfaces[i]));
					}
					Image interfaceImage = PDERuntimePluginImages
							.get(PDERuntimePluginImages.IMG_INTERFACE_OBJ);
					text.setImage("interface", interfaceImage); //$NON-NLS-1$
				}

				buffer.append("</form>"); //$NON-NLS-1$

				if (!bundleByClassName.isEmpty()) {
					text.addHyperlinkListener(new HyperlinkAdapter() {

						public void linkActivated(HyperlinkEvent e) {
							String href = (String) e.getHref();
							SpyIDEUtil.openClass(((Bundle) bundleByClassName
									.get(href)).getSymbolicName(), href);
							close();
						}

					});
				}

				text.setText(buffer.toString(), true, false);
			}
		}
	}

	protected Point getInitialLocation(Point initialSize) {
		if (fAnchor == null) {
			return super.getInitialLocation(initialSize);
		}
		Point point = fAnchor;
		Rectangle monitor = getShell().getMonitor().getClientArea();
		if (monitor.width < point.x + initialSize.x) {
			point.x = Math.max(0, point.x - initialSize.x);
		}
		if (monitor.height < point.y + initialSize.y) {
			point.y = Math.max(0, point.y - initialSize.y);
		}
		return point;
	}
	
	public boolean close() {
		if (toolkit != null) {
			if (toolkit.getColors() != null) {
				toolkit.dispose();
			}
		}
		return super.close();
	}
	protected Control getFocusControl() {
		return this.composite;
	}

}
