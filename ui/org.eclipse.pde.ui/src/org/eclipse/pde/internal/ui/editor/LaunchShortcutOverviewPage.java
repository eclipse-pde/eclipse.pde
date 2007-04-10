/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public abstract class LaunchShortcutOverviewPage extends PDEFormPage implements IHyperlinkListener {

	public LaunchShortcutOverviewPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected final Section createStaticSection(FormToolkit toolkit, Composite parent, String text) {
		Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(text);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);
		return section;
	}
	
	protected final FormText createClient(Composite section, String content, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		text.addHyperlinkListener(this);
		return text;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.startsWith("launchShortcut.")) { //$NON-NLS-1$
			href = href.substring(15);
			int index = href.indexOf('.');
			if (index < 0)
				return;  // error.  Format of href should be launchShortcut.<mode>.<launchShortcutId>
			String mode = href.substring(0, index);
			String id = href.substring(index + 1); 
			getEditor().doSave(null);
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.debug.ui.launchShortcuts"); //$NON-NLS-1$
			for (int i = 0; i < elements.length; i++) {
				if (id.equals(elements[i].getAttribute("id"))) //$NON-NLS-1$
					try {
						ILaunchShortcut shortcut = (ILaunchShortcut)elements[i].createExecutableExtension("class"); //$NON-NLS-1$
						preLaunch();
						shortcut.launch(new StructuredSelection(getLaunchObject()), mode);
					} catch (CoreException e1) {
					}
			}
		}
	}
	
	// returns the object which will be passed to the launch shortcut in a StructuredSelection
	protected abstract Object getLaunchObject();

	// allows subclasses to do setup before launching the shortcut
	protected void preLaunch() {
	}
	
	// returns the indent for each launcher
	protected abstract short getIndent();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars()
				.getStatusLineManager();
		mng.setMessage(e.getLabel());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars()
				.getStatusLineManager();
		mng.setMessage(null);
	}
	
	protected final String getLauncherText(boolean osgi, String message) {
		IConfigurationElement[] elements = getLaunchers(osgi);
		
		StringBuffer buffer = new StringBuffer();
		String indent = Short.toString(getIndent());
		
		for (int i = 0; i < elements.length; i++) {
			String mode = elements[i].getAttribute("mode"); //$NON-NLS-1$
			buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
			buffer.append(mode);
			buffer.append("\" bindent=\"" + indent + "\"><a href=\"launchShortcut."); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append(mode);
			buffer.append('.');
			buffer.append(elements[i].getAttribute("id")); //$NON-NLS-1$
			buffer.append("\">"); //$NON-NLS-1$
			buffer.append(elements[i].getAttribute("label")); //$NON-NLS-1$
			buffer.append("</a></li>"); //$NON-NLS-1$
		}
		return NLS.bind(message, buffer.toString());
	}
	
	private IConfigurationElement[] getLaunchers(boolean osgi) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.launchShortcuts"); //$NON-NLS-1$
		// validate elements
		ArrayList list = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			String mode = elements[i].getAttribute("mode"); //$NON-NLS-1$
			if (mode != null && (mode.equals(ILaunchManager.RUN_MODE) || mode.equals(ILaunchManager.DEBUG_MODE) || mode.equals(ILaunchManager.PROFILE_MODE)) 
					&& elements[i].getAttribute("label") != null && elements[i].getAttribute("id") != null &&  //$NON-NLS-1$ //$NON-NLS-2$
					osgi == "true".equals(elements[i].getAttribute("osgi"))) //$NON-NLS-1$ //$NON-NLS-2$
				list.add(elements[i]);
		}
		
		// sort elements based on criteria specified in bug 172703
		elements = (IConfigurationElement[])list.toArray(new IConfigurationElement[list.size()]);
		Arrays.sort(elements, new Comparator() {

			public int compare(Object arg0, Object arg1) {
				int mode1 = getModeValue(((IConfigurationElement)arg0).getAttribute("mode")); //$NON-NLS-1$
				int mode2 = getModeValue(((IConfigurationElement)arg1).getAttribute("mode")); //$NON-NLS-1$
				if (mode1 != mode2)
					return mode1 - mode2;
				String label1 = ((IConfigurationElement)arg0).getAttribute("label"); //$NON-NLS-1$
				String label2 = ((IConfigurationElement)arg1).getAttribute("label"); //$NON-NLS-1$
				return label1.compareTo(label2);
			}
			
			private int getModeValue(String value) {
				if (value.equals(ILaunchManager.RUN_MODE))
					return 0;
				else if (value.equals(ILaunchManager.DEBUG_MODE))
					return 1;
				return 2; // has to be ILaunchManager.PROFILE_MODE
			}
			
		});
		return elements;
	}

}
