/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;

/**
 * This action toggles whether the Outline page links its selection to the
 * active editor.
 * 
 * @since 3.0
 */
public class ToggleLinkWithEditorAction extends Action {

	PDEFormEditor fEditor;

	public ToggleLinkWithEditorAction(PDEFormEditor editor) {
		super(PDEPlugin.getResourceString("ToggleLinkWithEditorAction.label")); //$NON-NLS-1$
		boolean isLinkingEnabled = PDEPlugin.getDefault().getPreferenceStore()
				.getBoolean("ToggleLinkWithEditorAction.isChecked"); //$NON-NLS-1$
		setChecked(isLinkingEnabled);
		fEditor = editor;
		setToolTipText(PDEPlugin
				.getResourceString("ToggleLinkWithEditorAction.toolTip")); //$NON-NLS-1$
		setDescription(PDEPlugin
				.getResourceString("ToggleLinkWithEditorAction.description")); //$NON-NLS-1$
		setImageDescriptor(PDEPluginImages.DESC_LINK_WITH_EDITOR);
		setDisabledImageDescriptor(PDEPluginImages.DESC_LINK_WITH_EDITOR_DISABLED);
	}

	public void run() {
		PDEPlugin.getDefault().getPreferenceStore().setValue(
				"ToggleLinkWithEditorAction.isChecked", isChecked()); //$NON-NLS-1$
		if (isChecked())
			fEditor.synchronizeOutlinePage();
	}
}
