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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileNameFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConfigurationSection extends PDESection {

	private Button fDefault;
	private Button fCustom;
	private FormEntry fCustomEntry;
	private boolean fBlockChanges;

	private static final String[] TAB_LABELS = { "linux", "macosx", "win32" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final String[] TAB_OS = { Platform.OS_LINUX, Platform.OS_MACOSX, Platform.OS_WIN32 };

	private CTabFolder fTabFolder;
	private int fLastTab;

	public ConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(sectionData);

		section.setText(PDEUIMessages.ConfigurationSection_title);
		section.setDescription(PDEUIMessages.ConfigurationSection_desc);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		gd.horizontalSpan = 3;
		gd.grabExcessHorizontalSpace = true;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		fTabFolder.addSelectionListener(widgetSelectedAdapter(e -> {
			if (fCustomEntry.isDirty())
				fCustomEntry.commit();
			refresh();
		}));
		fTabFolder.setUnselectedImageVisible(false);

		fDefault = toolkit.createButton(client, PDEUIMessages.ConfigurationSection_default, SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		fDefault.setLayoutData(gd);
		fDefault.setEnabled(isEditable());
		fDefault.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockChanges) {
				boolean selected = fDefault.getSelection();
				IConfigurationFileInfo info = getConfigurationFileInfo();
				String os = getOS(fLastTab);
				info.setUse(os, selected ? "default" : "custom"); //$NON-NLS-1$ //$NON-NLS-2$
				info.setPath(os, selected == true ? null : fCustomEntry.getValue());
				fCustomEntry.setValue(selected == true ? null : fCustomEntry.getValue(), true);
				fCustomEntry.setEditable(!selected);
			}
		}));

		fCustom = toolkit.createButton(client, PDEUIMessages.ConfigurationSection_existing, SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		fCustom.setLayoutData(gd);
		fCustom.setEnabled(isEditable());

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fCustomEntry = new FormEntry(client, toolkit, PDEUIMessages.ConfigurationSection_file, PDEUIMessages.ConfigurationSection_browse, isEditable(), 35); //
		BidiUtils.applyBidiProcessing(fCustomEntry.getText(), StructuredTextTypeHandlerFactory.FILE);
		fCustomEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (!fBlockChanges) {
					IConfigurationFileInfo info = getConfigurationFileInfo();
					String os = getOS(fLastTab);
					info.setUse(os, "custom"); //$NON-NLS-1$
					info.setPath(os, entry.getValue());
				}
			}

			@Override
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				handleOpen();
			}
		});
		fCustomEntry.setEditable(isEditable());

		createTabs();
		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	private void handleBrowse() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ConfigurationSection_selection);
		dialog.setMessage(PDEUIMessages.ConfigurationSection_message);
		dialog.addFilter(new FileNameFilter("config.ini")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			fCustomEntry.setValue(file.getFullPath().toString());
		}
	}

	@Override
	public void refresh() {
		fBlockChanges = true;
		fLastTab = fTabFolder.getSelectionIndex();
		IConfigurationFileInfo info = getConfigurationFileInfo();
		String os = getOS(fLastTab);
		fDefault.setEnabled(isEditable());
		if (info == null) {
			fDefault.setSelection(true);
			fCustomEntry.setEditable(false);
		} else {
			boolean custom = "custom".equals(info.getUse(os)); //$NON-NLS-1$
			fDefault.setSelection(!custom);
			fCustom.setSelection(custom);
			fCustomEntry.setValue(custom == true ? info.getPath(os) : null, true);
			fCustomEntry.setEditable(isEditable() && custom);
		}
		super.refresh();
		fBlockChanges = false;
	}

	private IConfigurationFileInfo getConfigurationFileInfo() {
		IConfigurationFileInfo info = getProduct().getConfigurationFileInfo();
		if (info == null) {
			info = getModel().getFactory().createConfigFileInfo();
			getProduct().setConfigurationFileInfo(info);
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
	public void commit(boolean onSave) {
		fCustomEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fCustomEntry.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	private void handleOpen() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(fCustomEntry.getValue());
		if (path.isEmpty()) {
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); //
			return;
		}
		IResource resource = root.findMember(path);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile) resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); //
		} catch (PartInitException e) {
		}
	}

	@Override
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
		// This section can get disposed if the configuration is changed from
		// plugins to features or vice versa.  Subsequently, the configuration
		// page is removed and readded.  In this circumstance, abort the
		// refresh
		if (fCustomEntry.getText().isDisposed()) {
			return;
		}
		// Perform the refresh
		refresh();
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fCustomEntry.getText());
	}

	private void createTabs() {
		for (String tabLabel : TAB_LABELS) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(tabLabel);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);

		String currentTarget = TargetPlatform.getOS();

		if (Platform.OS_WIN32.equals(currentTarget)) {
			fTabFolder.setSelection(2);
		} else if (Platform.OS_MACOSX.equals(currentTarget)) {
			fTabFolder.setSelection(1);
		}
	}

	private String getOS(int tab) {
		if (tab >= 0 && tab < TAB_OS.length) {
			return TAB_OS[tab];
		}
		return null;
	}

}
