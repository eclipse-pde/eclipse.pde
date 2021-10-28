/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.preferencepage;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.pde.spy.preferences.PreferenceSpyConfiguration;
import org.eclipse.pde.spy.preferences.constants.PreferenceConstants;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class TracePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public TracePreferencePage() {
		super(GRID);
		setPreferenceStore(PreferenceSpyConfiguration.getPreferenceStore());
		setDescription(Messages.TracePreferencePage_Settings_for_preference_spy);
	}

	/**
	 * Creates the field editors
	 */
	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.TRACE_PREFERENCES, Messages.TracePreferencePage_Trace_preference_values,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.HIERARCHICAL_LAYOUT, Messages.TracePreferencePage_Use_hierarchical_layout_in_the_tree,
				getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
