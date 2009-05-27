/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

public abstract class LaunchShortcutOverviewPage extends PDEFormPage implements IHyperlinkListener {

	public LaunchShortcutOverviewPage(PDELauncherFormEditor editor, String id, String title) {
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
		getPDELauncherEditor().launch(href, getPDELauncherEditor().getPreLaunchRunnable(), getPDELauncherEditor().getLauncherHelper().getLaunchObject());
	}

	// returns the indent for each launcher
	protected abstract short getIndent();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}

	protected final String getLauncherText(boolean osgi, String message) {
		IConfigurationElement[][] elements = getPDELauncherEditor().getLaunchers(osgi);

		StringBuffer buffer = new StringBuffer();
		String indent = Short.toString(getIndent());

		for (int i = 0; i < elements.length; i++) {
			for (int j = 0; j < elements[i].length; j++) {
				String mode = elements[i][j].getAttribute("mode"); //$NON-NLS-1$
				buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
				buffer.append(mode);
				buffer.append("\" bindent=\"" + indent + "\"><a href=\"launchShortcut."); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(mode);
				buffer.append('.');
				buffer.append(elements[i][j].getAttribute("id")); //$NON-NLS-1$
				buffer.append("\">"); //$NON-NLS-1$
				buffer.append(elements[i][j].getAttribute("label")); //$NON-NLS-1$
				buffer.append("</a></li>"); //$NON-NLS-1$
			}
		}
		return NLS.bind(message, buffer.toString());
	}

	protected PDELauncherFormEditor getPDELauncherEditor() {
		return (PDELauncherFormEditor) getPDEEditor();
	}
}
