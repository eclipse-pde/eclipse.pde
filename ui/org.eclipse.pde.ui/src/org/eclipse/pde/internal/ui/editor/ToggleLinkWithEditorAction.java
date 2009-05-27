/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.*;

/**
 * This action toggles whether the Outline page links its selection to the
 * active editor.
 * 
 * @since 3.0
 */
public class ToggleLinkWithEditorAction extends Action {

	PDEFormEditor fEditor;

	public ToggleLinkWithEditorAction(PDEFormEditor editor) {
		super(PDEUIMessages.ToggleLinkWithEditorAction_label);
		boolean isLinkingEnabled = PDEPlugin.getDefault().getPreferenceStore().getBoolean("ToggleLinkWithEditorAction.isChecked"); //$NON-NLS-1$
		setChecked(isLinkingEnabled);
		fEditor = editor;
		setToolTipText(PDEUIMessages.ToggleLinkWithEditorAction_toolTip);
		setDescription(PDEUIMessages.ToggleLinkWithEditorAction_description);
		setImageDescriptor(PDEPluginImages.DESC_LINK_WITH_EDITOR);
		setDisabledImageDescriptor(PDEPluginImages.DESC_LINK_WITH_EDITOR_DISABLED);
	}

	public void run() {
		PDEPlugin.getDefault().getPreferenceStore().setValue("ToggleLinkWithEditorAction.isChecked", isChecked()); //$NON-NLS-1$
		if (isChecked())
			fEditor.synchronizeOutlinePage();
	}
}
