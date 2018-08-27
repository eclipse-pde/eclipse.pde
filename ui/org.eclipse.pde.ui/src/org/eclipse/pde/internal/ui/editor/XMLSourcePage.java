/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public abstract class XMLSourcePage extends PDEProjectionSourcePage {

	public XMLSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setRangeIndicator(new DefaultRangeIndicator());
	}

	@Override
	public boolean canLeaveThePage() {
		return true;
	}

	@Override
	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		String[] more = new String[ids.length + 1];
		more[0] = "org.eclipse.pde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

	@Override
	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(IColorManager colorManager) {
		if (getEditor() instanceof ManifestEditor)
			return new PluginXMLConfiguration(colorManager, this);
		return new XMLConfiguration(colorManager, this);
	}
}
