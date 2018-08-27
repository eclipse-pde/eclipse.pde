/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void linkActivated(HyperlinkEvent e) {
		// target href takes the form of launchShortcut.<mode>.<id>
		String href = (String) e.getHref();
		int modeStart = href.indexOf('.');
		if (modeStart != -1) {
			int modeEnd = href.indexOf('.', modeStart + 1);
			if (modeEnd != -1) {
				getPDELauncherEditor().launch(href.substring(modeEnd + 1), href.substring(modeStart + 1, modeEnd), getPDELauncherEditor().getPreLaunchRunnable(), getPDELauncherEditor().getLauncherHelper().getLaunchObject());
			}
		}
	}

	// returns the indent for each launcher
	protected abstract short getIndent();

	@Override
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);
	}

	protected final String getLauncherText(boolean osgi, String message) {
		IConfigurationElement[][] launchElementsByMode = getPDELauncherEditor().getLaunchers(osgi);

		StringBuilder buffer = new StringBuilder();
		String indent = Short.toString(getIndent());

		for (IConfigurationElement[] elements : launchElementsByMode) {
			for (IConfigurationElement element : elements) {
				String mode = element.getAttribute("mode"); //$NON-NLS-1$
				buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
				buffer.append(mode);
				buffer.append("\" bindent=\"" + indent + "\"><a href=\"launchShortcut."); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(mode);
				buffer.append('.');
				buffer.append(element.getAttribute("id")); //$NON-NLS-1$
				buffer.append("\">"); //$NON-NLS-1$
				buffer.append(element.getAttribute("label")); //$NON-NLS-1$
				buffer.append("</a></li>"); //$NON-NLS-1$
			}
		}
		return NLS.bind(message, buffer.toString());
	}

	protected PDELauncherFormEditor getPDELauncherEditor() {
		return (PDELauncherFormEditor) getPDEEditor();
	}
}
