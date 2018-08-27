/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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

	@Override
	public void run() {
		PDEPlugin.getDefault().getPreferenceStore().setValue("ToggleLinkWithEditorAction.isChecked", isChecked()); //$NON-NLS-1$
		if (isChecked())
			fEditor.synchronizeOutlinePage();
	}
}
