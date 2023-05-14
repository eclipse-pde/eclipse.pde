/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.shared.target.TargetContentsGroup;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section for editing the content of the target (bundle containers) in the target definition editor
 * @see DefinitionPage
 * @see TargetEditor
 */
public class ContentSection extends SectionPart {

	private TargetContentsGroup fContentGroup;
	private TargetEditor fEditor;

	public ContentSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 *
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.horizontalSpan = 2;
		section.setLayoutData(sectionData);
		section.setText(PDEUIMessages.ContentSection_0);

		section.setDescription(PDEUIMessages.ContentSection_1);
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		client.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		fContentGroup = TargetContentsGroup.createInForm(client, toolkit);
		fEditor.getTargetChangedListener().setContentTree(fContentGroup);
		fContentGroup.addTargetChangedListener(fEditor.getTargetChangedListener());
		fContentGroup.addTargetChangedListener((definition, source, resolve, forceResolve) -> {
			if (source instanceof TargetContentsGroup) {
				if (((TargetContentsGroup) source).isFeatureModeEnabled())
					section.setDescription(PDEUIMessages.ContentSection_2);
				else
					section.setDescription(PDEUIMessages.ContentSection_1);
			}
			markDirty();
		});

		toolkit.paintBordersFor(client);

		section.setClient(client);
	}

	@Override
	public void refresh() {
		// Use the change listener as it can check if we are resolved, resolving, or cancelled bug 264908
		fEditor.getTargetChangedListener().contentsChanged(getTarget(), this, false, false);
		super.refresh();
	}

}
