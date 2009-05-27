/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ArgumentsSection extends PDESection {

	private static final String[] TAB_LABELS = new String[5];
	static {
		TAB_LABELS[IArgumentsInfo.L_ARGS_ALL] = PDEUIMessages.ArgumentsSection_allPlatforms;
		TAB_LABELS[IArgumentsInfo.L_ARGS_LINUX] = "linux"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_MACOS] = "macosx"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_SOLAR] = "solaris"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_WIN32] = "win32"; //$NON-NLS-1$
	}

	private FormEntry fVMArgs;
	private FormEntry fProgramArgs;
	private CTabFolder fTabFolder;
	private int fLastTab;

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.ArgumentsSection_title);
		section.setDescription(PDEUIMessages.ArgumentsSection_desc);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fProgramArgs.isDirty())
					fProgramArgs.commit();
				if (fVMArgs.isDirty())
					fVMArgs.commit();
				refresh();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		fProgramArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_program, SWT.MULTI | SWT.WRAP);
		fProgramArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				IArgumentsInfo info = getLauncherArguments();
				info.setProgramArguments(entry.getValue().trim(), fLastTab);
			}
		});
		fProgramArgs.setEditable(isEditable());

		fVMArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_vm, SWT.MULTI | SWT.WRAP);
		fVMArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fVMArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				IArgumentsInfo info = getLauncherArguments();
				info.setVMArguments(entry.getValue().trim(), fLastTab);
			}
		});
		fVMArgs.setEditable(isEditable());

		createTabs();
		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);
	}

	public void refresh() {
		fLastTab = fTabFolder.getSelectionIndex();
		fProgramArgs.setValue(getLauncherArguments().getProgramArguments(fLastTab), true);
		fVMArgs.setValue(getLauncherArguments().getVMArguments(fLastTab), true);
		super.refresh();
	}

	public void commit(boolean onSave) {
		fProgramArgs.commit();
		fVMArgs.commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fProgramArgs.cancelEdit();
		fVMArgs.cancelEdit();
		super.cancelEdit();
	}

	private IArgumentsInfo getLauncherArguments() {
		IArgumentsInfo info = getProduct().getLauncherArguments();
		if (info == null) {
			info = getModel().getFactory().createLauncherArguments();
			getProduct().setLauncherArguments(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

}
