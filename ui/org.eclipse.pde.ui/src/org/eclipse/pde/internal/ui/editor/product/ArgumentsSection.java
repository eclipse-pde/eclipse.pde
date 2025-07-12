/*******************************************************************************
 *  Copyright (c) 2005, 2025 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboViewerPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ArgumentsSection extends PDESection {

	private static final String[] TAB_LABELS = new String[5];
	static {
		TAB_LABELS[IArgumentsInfo.L_ARGS_ALL] = PDEUIMessages.ArgumentsSection_allPlatforms;
		TAB_LABELS[IArgumentsInfo.L_ARGS_FREEBSD] = "freebsd"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_LINUX] = "linux"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_MACOS] = "macosx"; //$NON-NLS-1$
		TAB_LABELS[IArgumentsInfo.L_ARGS_WIN32] = "win32"; //$NON-NLS-1$
	}

	private static final String[] TAB_ARCHLABELS = new String[8];
	static {
		TAB_ARCHLABELS[IArgumentsInfo.L_ARGS_ALL] = PDEUIMessages.ArgumentsSection_allArch;
		TAB_ARCHLABELS[IArgumentsInfo.L_ARGS_ARCH_X86] = IArgumentsInfo.ARCH_X86;
		TAB_ARCHLABELS[IArgumentsInfo.L_ARGS_ARCH_X86_64] = Platform.ARCH_X86_64;
	}

	private FormEntry fVMArgs;
	private FormEntry fProgramArgs;
	private FormEntry fPreviewArgs;
	private CTabFolder fTabFolder;
	private ComboViewerPart fArchCombo;
	private int fLastTab;
	private final int[] fLastArch = {0, 0, 0, 0, 0}; // default arch index is "All" (0)

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
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

		fTabFolder.addSelectionListener(widgetSelectedAdapter(e -> {
			if (fProgramArgs.isDirty()) {
				fProgramArgs.commit();
			}
			if (fVMArgs.isDirty()) {
				fVMArgs.commit();
			}
			refresh();
			fArchCombo.select(fLastArch[fLastTab]);
		}));
		createTabs();

		Composite archParent = toolkit.createComposite(client);
		archParent.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		archParent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		toolkit.createLabel(archParent, PDEUIMessages.ArgumentsSection_architecture);
		fArchCombo = new ComboViewerPart();
		fArchCombo.createControl(archParent, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fArchCombo.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		fArchCombo.setItems(Arrays.stream(TAB_ARCHLABELS).filter(Objects::nonNull).toArray(String[]::new));
		Control archComboControl = fArchCombo.getControl();
		if (archComboControl instanceof Combo) {
			((Combo) archComboControl).select(fLastArch[fLastTab]);
		} else {
			((CCombo) archComboControl).select(fLastArch[fLastTab]);
		}
		fArchCombo.addSelectionChangedListener(event -> {
			if (fProgramArgs.isDirty()) {
				fProgramArgs.commit();
			}
			if (fVMArgs.isDirty()) {
				fVMArgs.commit();
			}
			// remember the change in combo for currently selected platform
			Control fArchComboControl = fArchCombo.getControl();
			if (fArchComboControl instanceof Combo) {
				fLastArch[fLastTab] = ((Combo) fArchComboControl).getSelectionIndex();
			} else {
				fLastArch[fLastTab] = ((CCombo) fArchComboControl).getSelectionIndex();
			}

			refresh();
		});

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		fProgramArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_program, SWT.MULTI | SWT.WRAP);
		fProgramArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				IArgumentsInfo info = getLauncherArguments();
				info.setProgramArguments(entry.getValue().trim(), fLastTab, fLastArch[fLastTab]);
				updateArgumentPreview(info);
			}
		});
		fProgramArgs.setEditable(isEditable());

		fVMArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_vm, SWT.MULTI | SWT.WRAP);
		fVMArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fVMArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				IArgumentsInfo info = getLauncherArguments();
				info.setVMArguments(entry.getValue().trim(), fLastTab, fLastArch[fLastTab]);
				updateArgumentPreview(info);
			}
		});
		fVMArgs.setEditable(isEditable());

		fPreviewArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_preview, SWT.MULTI | SWT.WRAP);
		fPreviewArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fPreviewArgs.setEditable(false);

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	private void createTabs() {
		for (String tabLabel : TAB_LABELS) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(tabLabel);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);
	}

	@Override
	public void refresh() {
		fLastTab = fTabFolder.getSelectionIndex();
		IArgumentsInfo launcherArguments = getLauncherArguments();
		fProgramArgs.setValue(launcherArguments.getProgramArguments(fLastTab, fLastArch[fLastTab]), true);
		fVMArgs.setValue(launcherArguments.getVMArguments(fLastTab, fLastArch[fLastTab]), true);
		updateArgumentPreview(launcherArguments);
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		fProgramArgs.commit();
		fVMArgs.commit();
		super.commit(onSave);
	}

	@Override
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

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	private void updateArgumentPreview(IArgumentsInfo launcherArguments) {
		StringBuilder buffer = new StringBuilder();
		String delim = System.lineSeparator();
		String args = launcherArguments.getCompleteProgramArguments(TAB_LABELS[fLastTab], TAB_ARCHLABELS[fLastArch[fLastTab]]);
		if (args.length() > 0) {
			buffer.append(PDEUIMessages.ArgumentsSection_program);
			buffer.append(delim);
			buffer.append(args);
			buffer.append(delim);
			buffer.append(delim);
		}
		args = launcherArguments.getCompleteVMArguments(TAB_LABELS[fLastTab], TAB_ARCHLABELS[fLastArch[fLastTab]]);
		if (args.length() > 0) {
			buffer.append(PDEUIMessages.ArgumentsSection_vm);
			buffer.append(delim);
			buffer.append(args);
		}
		fPreviewArgs.setValue(buffer.toString());
	}

}
