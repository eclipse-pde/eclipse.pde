/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.core.target.ITargetDefinition;

import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.shared.target.ArgumentsFromContainerSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing VM and program arguments in the target definition editor
 * @see EnvironmentPage
 * @see TargetEditor
 */
public class ArgumentsSection extends SectionPart {

	private CTabFolder fTabFolder;
	private FormEntry fProgramArguments;
	private FormEntry fVMArguments;
	private Image fImage;
	private TargetEditor fEditor;

	public ArgumentsSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		fImage = PDEPluginImages.DESC_ARGUMENT_TAB.createImage();
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
		section.setText(PDEUIMessages.ArgumentsSection_editorTitle);
		section.setDescription(PDEUIMessages.ArgumentsSection_description);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		client.setLayoutData(gd);

		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		gd = new GridData(GridData.FILL_BOTH);
		fTabFolder.setLayoutData(gd);
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		Composite programComp = toolkit.createComposite(fTabFolder);
		programComp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		programComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArguments = new FormEntry(programComp, toolkit, PDEUIMessages.ArgumentsSection_0, SWT.MULTI | SWT.WRAP);
		fProgramArguments.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArguments.setFormEntryListener(new SimpleFormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String value = entry.getValue().trim();
				getTarget().setProgramArguments(value.length() > 0 ? value : null);
			}
		});
		Button variables = toolkit.createButton(programComp, PDEUIMessages.ArgumentsSection_variableButtonTitle, SWT.NONE);
		variables.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		variables.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getSection().getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fProgramArguments.getText().insert(variable);
				}
			}
		});
		CTabItem programTab = new CTabItem(fTabFolder, SWT.NULL);
		programTab.setText(PDEUIMessages.ArgumentsSection_programTabLabel);
		programTab.setImage(fImage);
		programTab.setControl(programComp);
		toolkit.paintBordersFor(programComp);

		Composite vmComp = toolkit.createComposite(fTabFolder);
		vmComp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		vmComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		fVMArguments = new FormEntry(vmComp, toolkit, PDEUIMessages.ArgumentsSection_1, SWT.MULTI | SWT.WRAP);
		fVMArguments.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fVMArguments.setFormEntryListener(new SimpleFormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String value = entry.getValue().trim();
				getTarget().setVMArguments(value.length() > 0 ? value : null);
			}
		});

		Composite buttons = new Composite(vmComp, SWT.NONE);
		GridLayout layout = FormLayoutFactory.createSectionClientGridLayout(false, 2);
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginRight = 0;
		layout.marginLeft = 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		Button vmArgs = toolkit.createButton(buttons, PDEUIMessages.ArgumentsSection_argumentsButtonTitle, SWT.NONE);
		vmArgs.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		vmArgs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ArgumentsFromContainerSelectionDialog dialog = new ArgumentsFromContainerSelectionDialog(getSection().getShell(), getTarget());
				if (dialog.open() == Window.OK) {
					String[] args = dialog.getSelectedArguments();
					if (args != null && args.length > 0) {
						StringBuffer resultBuffer = new StringBuffer();
						for (int index = 0; index < args.length; ++index) {
							resultBuffer.append(args[index] + " "); //$NON-NLS-1$
						}
						fVMArguments.getText().insert(resultBuffer.toString());
					}
				}
			}
		});

		variables = toolkit.createButton(buttons, PDEUIMessages.ArgumentsSection_variableButtonTitle, SWT.NONE);
		variables.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		variables.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getSection().getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fVMArguments.getText().insert(variable);
				}
			}
		});
		CTabItem vmTab = new CTabItem(fTabFolder, SWT.NULL);
		vmTab.setText(PDEUIMessages.ArgumentsSection_vmTabLabel);
		vmTab.setImage(fImage);
		vmTab.setControl(vmComp);
		toolkit.paintBordersFor(vmComp);

		fTabFolder.setSelection(programTab);

		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fProgramArguments.setValue(getTarget().getProgramArguments(), true);
		fVMArguments.setValue(getTarget().getVMArguments(), true);
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fProgramArguments.commit();
		fVMArguments.commit();
		super.commit(onSave);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		if (fImage != null)
			fImage.dispose();
		super.dispose();
	}

}
