/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.shared.target.BundleContainerTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 *
 */
public class TargetDefinitionContentPage extends TargetDefinitionPage {

	private Text fNameText;
	private Text fDescriptionText;
	private BundleContainerTable fTable;

	/**
	 * @param pageName
	 */
	public TargetDefinitionContentPage(ITargetDefinition target) {
		super("targetContent", target); //$NON-NLS-1$
		setTitle(PDEUIMessages.TargetDefinitionContentPage_1);
		setDescription(PDEUIMessages.TargetDefinitionContentPage_2);
		setImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, true));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEUIMessages.TargetDefinitionContentPage_3);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.TargetDefinitionContentPage_4);
		GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		label.setLayoutData(gridData);

		fNameText = new Text(group, SWT.BORDER);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		fNameText.setLayoutData(gridData);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setName(fNameText.getText().trim());
			}
		});

		label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.TargetDefinitionContentPage_5);
		gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
		label.setLayoutData(gridData);

		fDescriptionText = new Text(group, SWT.BORDER);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		fDescriptionText.setLayoutData(gridData);
		fDescriptionText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setDescription(fDescriptionText.getText().trim());
			}
		});

		Group content = new Group(comp, SWT.NONE);
		content.setText(PDEUIMessages.TargetDefinitionContentPage_6);
		content.setLayout(new GridLayout(2, false));
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.widthHint = 200;
		content.setLayoutData(gridData);

		fTable = BundleContainerTable.createTableInDialog(content);
		setControl(comp);
		targetChanged(getTargetDefinition());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionPage#targetChanged()
	 */
	protected void targetChanged(ITargetDefinition definition) {
		super.targetChanged(definition);
		if (definition != null) {
			String name = definition.getName();
			if (name == null) {
				name = ""; //$NON-NLS-1$
			}
			fNameText.setText(name);
			String des = definition.getDescription();
			if (des == null) {
				des = ""; //$NON-NLS-1$
			}
			fDescriptionText.setText(des);
			fTable.setInput(definition);
		}
	}
}
