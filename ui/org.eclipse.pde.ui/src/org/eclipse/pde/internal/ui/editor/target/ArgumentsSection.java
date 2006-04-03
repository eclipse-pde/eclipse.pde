/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ArgumentsSection extends PDESection {
	
	private static final String[] TAB_LABELS = new String[2];
	static {
		TAB_LABELS[0] = PDEUIMessages.ArgumentsSection_programTabLabel;
		TAB_LABELS[1] = PDEUIMessages.ArgumentsSection_vmTabLabel;
	}
	
	private CTabFolder fTabFolder;
	private FormEntry fArgument;
	private int fLastTab;
	private Image fImage;

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		fImage = PDEPluginImages.DESC_ARGUMENT_TAB.createImage();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ArgumentsSection_editorTitle);
		section.setDescription(PDEUIMessages.ArgumentsSection_description);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 1;
		section.setLayoutData(data);
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		client.setLayoutData(gd);
		
		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor1,
				selectedColor2, toolkit.getColors().getBackground() },
				new int[] { 50, 100 }, true);
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fArgument.isDirty())
					fArgument.commit();
				refresh();
			}
		});

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		fArgument = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_argumentsTextLabel, SWT.MULTI|SWT.WRAP); 
		fArgument.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fArgument.setFormEntryListener(new FormEntryAdapter(this, actionBars) {			
			public void textValueChanged(FormEntry entry) {
				if (fLastTab == 0)
					getArgumentInfo().setProgramArguments(fArgument.getValue());
				else
					getArgumentInfo().setVMArguments(fArgument.getValue());
			}
		});
		
		Button variables = toolkit.createButton(client, PDEUIMessages.ArgumentsSection_variableButtonTitle, SWT.NONE);
		variables.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		variables.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getSection().getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
                    fArgument.getText().insert(variable);
				}
			}
		});

		createTabs();
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	
	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(fImage);
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);
	}
	
	private IArgumentsInfo getArgumentInfo() {
		IArgumentsInfo info = getTarget().getArguments();
		if (info == null) {
			info = getModel().getFactory().createArguments();
			getTarget().setArguments(info);
		}
		return info;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public void refresh() {
		fLastTab = fTabFolder.getSelectionIndex();
		if (fLastTab == 0)
			fArgument.setValue(getArgumentInfo().getProgramArguments(), true);
		else
			fArgument.setValue(getArgumentInfo().getVMArguments(), true);
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fArgument.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fArgument.cancelEdit();
		super.cancelEdit();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}
	
	public void dispose() {
		if (fImage != null)
			fImage.dispose();
		super.dispose();
	}
	
}
