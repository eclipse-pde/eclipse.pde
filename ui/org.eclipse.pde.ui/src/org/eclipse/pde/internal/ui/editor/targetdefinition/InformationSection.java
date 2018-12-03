/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.validation.ControlValidationUtility;
import org.eclipse.pde.internal.ui.editor.validation.TextValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing target name and description in the target definition editor
 * @see DefinitionPage
 * @see TargetEditor
 */
public class InformationSection extends SectionPart {

	private Text fNameText;
	private TargetEditor fEditor;

	private TextValidator fNameTextValidator;
	private FormPage fPage;

	public InformationSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), ExpandableComposite.TITLE_BAR);
		fPage = page;
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
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		section.setText(PDEUIMessages.InformationSection_0);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		fNameText = toolkit.createText(client, getTarget().getName());
		fNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNameText.addModifyListener(e -> {
			String value = fNameText.getText().trim();
			getTarget().setName(value.length() > 0 ? value : null);
			markDirty();
		});

		fNameTextValidator = new TextValidator(fPage.getManagedForm(), fNameText, null, true) {

			@Override
			protected boolean autoEnable() {
				if (getText().getEditable() == false) {
					return false;
				}
				return true;
			}

			@Override
			protected boolean validateControl() {
				return ControlValidationUtility.validateRequiredField(fNameText.getText(), fNameTextValidator, IMessageProvider.ERROR);
			}
		};
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	@Override
	public void refresh() {
		fNameText.setText(getTarget().getName() != null ? getTarget().getName() : ""); //$NON-NLS-1$
		super.refresh();
	}

}
