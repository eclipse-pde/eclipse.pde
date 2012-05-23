/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.validation.TextValidator;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class LauncherSection extends PDESection {

	private TextValidator[] fMultipleWinIconValidator;

	private TextValidator fSingleWinIconValidator;

	private static final String[] F_WIN_ICON_LABELS = new String[] {PDEUIMessages.LauncherSection_Low16, PDEUIMessages.LauncherSection_High16, PDEUIMessages.LauncherSection_32Low, PDEUIMessages.LauncherSection_32High, PDEUIMessages.LauncherSection_48Low, PDEUIMessages.LauncherSection_48High, PDEUIMessages.LauncherSection_256High};
	public static final int[] F_WIN_ICON_DEPTHS = new int[] {8, 32, 8, 32, 8, 32, 32};
	public static final int[][] F_WIN_ICON_DIMENSIONS = new int[][] { {16, 16}, {16, 16}, {32, 32}, {32, 32}, {48, 48}, {48, 48}, {256, 256}};
	private static final String[] F_WIN_ICON_IDS = new String[] {ILauncherInfo.WIN32_16_LOW, ILauncherInfo.WIN32_16_HIGH, ILauncherInfo.WIN32_32_LOW, ILauncherInfo.WIN32_32_HIGH, ILauncherInfo.WIN32_48_LOW, ILauncherInfo.WIN32_48_HIGH, ILauncherInfo.WIN32_256_HIGH};

	private FormEntry fNameEntry;
	private ArrayList fIcons = new ArrayList();
	private Button fIcoButton;
	private Button fBmpButton;
	private CTabFolder fTabFolder;
	private Composite fNotebook;
	private StackLayout fNotebookLayout;
	private Composite fLinuxSection;
	private Composite fMacSection;
	private Composite fSolarisSection;
	private Composite fWin32Section;

	class IconEntry extends FormEntry {
		String fIconId;

		public IconEntry(Composite parent, FormToolkit toolkit, String labelText, String iconId) {
			super(parent, toolkit, labelText, PDEUIMessages.LauncherSection_browse, isEditable(), 20);
			fIconId = iconId;
			addEntryFormListener();
			setEditable(isEditable());
		}

		private void addEntryFormListener() {
			IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
			setFormEntryListener(new FormEntryAdapter(LauncherSection.this, actionBars) {
				public void textValueChanged(FormEntry entry) {
					getLauncherInfo().setIconPath(fIconId, entry.getValue());
				}

				public void browseButtonSelected(FormEntry entry) {
					handleBrowse((IconEntry) entry);
				}

				public void linkActivated(HyperlinkEvent e) {
					openImage(IconEntry.this.getValue());
				}
			});
		}

		public String getIconId() {
			return fIconId;
		}
	}

	public LauncherSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.LauncherSection_title);
		section.setDescription(PDEUIMessages.LauncherSection_desc);

		Composite container = toolkit.createComposite(section);
		GridLayout layout = FormLayoutFactory.createSectionClientGridLayout(false, 2);
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fNameEntry = new FormEntry(container, toolkit, PDEUIMessages.LauncherSection_launcherName, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherInfo().setLauncherName(entry.getValue());
			}
		});
		fNameEntry.setEditable(isEditable());

		createLabel(container, toolkit, PDEUIMessages.LauncherSection_label, 2);

		fTabFolder = new CTabFolder(container, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = 2;
		gd.horizontalSpan = 2;
		fTabFolder.setLayoutData(gd);

		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, new int[] {100}, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);

		fNotebook = toolkit.createComposite(container);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fNotebook.setLayoutData(gd);
		fNotebookLayout = new StackLayout();
		fNotebook.setLayout(fNotebookLayout);

		fLinuxSection = addLinuxSection(fNotebook, toolkit);
		fMacSection = addMacSection(fNotebook, toolkit);
		fSolarisSection = addSolarisSection(fNotebook, toolkit);
		fWin32Section = addWin32Section(fNotebook, toolkit);

		createTabs();

		toolkit.paintBordersFor(container);
		section.setClient(container);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
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

	private void createTabs() {
		addTab("linux"); //$NON-NLS-1$
		addTab("macosx"); //$NON-NLS-1$
		addTab("solaris"); //$NON-NLS-1$
		addTab("win32"); //$NON-NLS-1$

		String currentTarget = TargetPlatform.getOS();
		if ("win32".equals(currentTarget)) { //$NON-NLS-1$
			fTabFolder.setSelection(3);
			fNotebookLayout.topControl = fWin32Section;
		} else if ("macosx".equals(currentTarget)) { //$NON-NLS-1$
			fTabFolder.setSelection(1);
			fNotebookLayout.topControl = fMacSection;
		} else if ("solaris".equals(currentTarget)) { //$NON-NLS-1$
			fTabFolder.setSelection(2);
			fNotebookLayout.topControl = fSolarisSection;
		} else {
			fTabFolder.setSelection(0);
			fNotebookLayout.topControl = fLinuxSection;
		}
	}

	private void addTab(String label) {
		CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
		item.setText(label);
		item.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
	}

	private Composite addWin32Section(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);

		fBmpButton = toolkit.createButton(comp, PDEUIMessages.LauncherSection_bmpImages, SWT.RADIO);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fBmpButton.setLayoutData(td);
		fBmpButton.setEnabled(isEditable());
		// Store all win icon validators
		fMultipleWinIconValidator = new TextValidator[F_WIN_ICON_LABELS.length];
		for (int i = 0; i < F_WIN_ICON_LABELS.length; i++) {
			final IconEntry ientry = new IconEntry(comp, toolkit, F_WIN_ICON_LABELS[i], F_WIN_ICON_IDS[i]);
			final int index = i;
			// Create validator
			fMultipleWinIconValidator[index] = new TextValidator(getManagedForm(), ientry.getText(), getProject(), true) {
				protected boolean validateControl() {
					return validateMultipleWinIcon(ientry, index);
				}
			};
			// Disable initially
			fMultipleWinIconValidator[index].setEnabled(false);
			// Validate on modify
			ientry.getText().addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					fMultipleWinIconValidator[index].validate();
				}
			});

			fIcons.add(ientry);
		}

		fIcoButton = toolkit.createButton(comp, PDEUIMessages.LauncherSection_ico, SWT.RADIO);
		td = new TableWrapData();
		td.colspan = 3;
		fIcoButton.setLayoutData(td);
		fIcoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fIcoButton.getSelection();
				getLauncherInfo().setUseWinIcoFile(selected);
				updateWinEntries(selected);
			}
		});
		fIcoButton.setEnabled(isEditable());

		final IconEntry ientry = new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_file, ILauncherInfo.P_ICO_PATH);
		// Create validator
		fSingleWinIconValidator = new TextValidator(getManagedForm(), ientry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateSingleWinIcon(ientry);
			}
		};
		// Disable initially
		fSingleWinIconValidator.setEnabled(false);

		fIcons.add(ientry);

		toolkit.paintBordersFor(comp);
		return comp;
	}

	private boolean validateSingleWinIcon(IconEntry ientry) {
		return EditorUtilities.imageEntryHasValidIco(fSingleWinIconValidator, ientry, getProduct());
	}

	private boolean validateMultipleWinIcon(IconEntry ientry, int index) {
		return EditorUtilities.imageEntryHasExactDepthAndSize(fMultipleWinIconValidator[index], ientry, getProduct(), F_WIN_ICON_DIMENSIONS[index][0], F_WIN_ICON_DIMENSIONS[index][1], F_WIN_ICON_DEPTHS[index]);
	}

	private void createLabel(Composite parent, FormToolkit toolkit, String text, int span) {
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		Layout layout = parent.getLayout();
		if (layout instanceof GridLayout) {
			GridData gd = new GridData();
			gd.horizontalSpan = span;
			label.setLayoutData(gd);
		} else if (layout instanceof TableWrapLayout) {
			TableWrapData td = new TableWrapData();
			td.colspan = span;
			label.setLayoutData(td);
		}
	}

	private Composite addLinuxSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_linuxLabel, 3);
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_icon, ILauncherInfo.LINUX_ICON));
		toolkit.paintBordersFor(comp);
		return comp;
	}

	private Composite addSolarisSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_solarisLabel, 3);

		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_large, ILauncherInfo.SOLARIS_LARGE));
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_medium, ILauncherInfo.SOLARIS_MEDIUM));
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_small, ILauncherInfo.SOLARIS_SMALL));
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_tiny, ILauncherInfo.SOLARIS_TINY));

		toolkit.paintBordersFor(comp);
		return comp;
	}

	private Composite addMacSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_macLabel, 3);
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_file, ILauncherInfo.MACOSX_ICON));
		toolkit.paintBordersFor(comp);
		return comp;
	}

	private Composite createComposite(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = layout.topMargin = layout.leftMargin = layout.rightMargin = 0;
		layout.numColumns = 3;
		comp.setLayout(layout);
		return comp;
	}

	public void refresh() {
		ILauncherInfo info = getLauncherInfo();
		fNameEntry.setValue(info.getLauncherName(), true);
		boolean useIco = info.usesWinIcoFile();
		fIcoButton.setSelection(useIco);
		fBmpButton.setSelection(!useIco);

		// Turn off auto message update until after values are set
		fSingleWinIconValidator.setRefresh(false);
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = (IconEntry) fIcons.get(i);
			entry.setValue(info.getIconPath(entry.getIconId()), true);
		}
		// Turn back on auto message update
		fSingleWinIconValidator.setRefresh(true);

		updateWinEntries(useIco);

		super.refresh();
	}

	private void updateWinEntries(boolean useIco) {
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = (IconEntry) fIcons.get(i);
			String id = entry.getIconId();
			if (id.equals(ILauncherInfo.P_ICO_PATH)) {
				boolean enabled = isEditable() && useIco;
				entry.setEditable(enabled);
			} else if (id.equals(ILauncherInfo.WIN32_16_HIGH) || id.equals(ILauncherInfo.WIN32_16_LOW) || id.equals(ILauncherInfo.WIN32_32_HIGH) || id.equals(ILauncherInfo.WIN32_32_LOW) || id.equals(ILauncherInfo.WIN32_48_HIGH) || id.equals(ILauncherInfo.WIN32_48_LOW) || id.equals(ILauncherInfo.WIN32_256_HIGH)) {
				entry.setEditable(isEditable() && !useIco);
			}
		}
		// Update validators
		updateWinEntryValidators(useIco);
	}

	/**
	 * @param useIco
	 */
	private void updateWinEntryValidators(boolean useIco) {
		// Turn off auto message update until after values are set
		fSingleWinIconValidator.setRefresh(false);
		// Update validator
		fSingleWinIconValidator.setEnabled(isEditable() && useIco);
		// Update validators
		for (int i = 0; i < fMultipleWinIconValidator.length; i++) {
			fMultipleWinIconValidator[i].setEnabled(isEditable() && !useIco);
		}
		// Turn back on auto message update
		fSingleWinIconValidator.setRefresh(true);
	}

	private ILauncherInfo getLauncherInfo() {
		ILauncherInfo info = getProduct().getLauncherInfo();
		if (info == null) {
			info = getModel().getFactory().createLauncherInfo();
			getProduct().setLauncherInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	public void commit(boolean onSave) {
		fNameEntry.commit();
		for (int i = 0; i < fIcons.size(); i++)
			((FormEntry) fIcons.get(i)).commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fNameEntry.cancelEdit();
		for (int i = 0; i < fIcons.size(); i++)
			((FormEntry) fIcons.get(i)).commit();
		super.cancelEdit();
	}

	private void handleBrowse(IconEntry entry) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.LauncherSection_dialogTitle);
		String extension = getExtension(entry.getIconId());
		dialog.setMessage(PDEUIMessages.LauncherSection_dialogMessage);
		dialog.addFilter(new FileExtensionFilter(extension));
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}

	private void openImage(String value) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		Path path = new Path(value);
		if (path.isEmpty()) {
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return;
		}
		IResource resource = root.findMember(new Path(value));
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile) resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); // 
		} catch (PartInitException e) {
		}
	}

	private String getExtension(String iconId) {
		if (iconId.equals(ILauncherInfo.LINUX_ICON))
			return "xpm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.MACOSX_ICON))
			return "icns"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_LARGE))
			return "l.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_MEDIUM))
			return "m.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_SMALL))
			return "s.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.SOLARIS_TINY))
			return "t.pm"; //$NON-NLS-1$
		if (iconId.equals(ILauncherInfo.P_ICO_PATH))
			return "ico"; //$NON-NLS-1$
		return "bmp"; //$NON-NLS-1$
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return (d.getFocusControl() instanceof Text);
	}

	private void updateTabSelection() {
		int index = fTabFolder.getSelectionIndex();
		Control oldPage = fNotebookLayout.topControl;
		switch (index) {
			case 0 :
				fNotebookLayout.topControl = fLinuxSection;
				break;
			case 1 :
				fNotebookLayout.topControl = fMacSection;
				break;
			case 2 :
				fNotebookLayout.topControl = fSolarisSection;
				break;
			case 3 :
				fNotebookLayout.topControl = fWin32Section;
				break;
		}
		if (oldPage != fNotebookLayout.topControl)
			fNotebook.layout();
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
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving 
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fNameEntry.getText());
	}
}
