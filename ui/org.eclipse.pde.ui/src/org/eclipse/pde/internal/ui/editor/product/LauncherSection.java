/*******************************************************************************
 * Copyright (c) 2005, 2025 IBM Corporation and others.
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
 *     SAP SE - support macOS bundle URL types
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IMacBundleUrlType;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.validation.TextValidator;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
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
	private final ArrayList<IconEntry> fIcons = new ArrayList<>();
	private Button fIcoButton;
	private Button fBmpButton;
	private CTabFolder fTabFolder;
	private Composite fNotebook;
	private StackLayout fNotebookLayout;
	private Composite fFreeBSDSection;
	private Composite fLinuxSection;
	private Composite fMacSection;
	private Composite fWin32Section;
	private MacBundleUrlTypesComposite fMacBundleUrlTypesComposite;

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
				@Override
				public void textValueChanged(FormEntry entry) {
					getLauncherInfo().setIconPath(fIconId, entry.getValue());
				}

				@Override
				public void browseButtonSelected(FormEntry entry) {
					handleBrowse((IconEntry) entry);
				}

				@Override
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

	@Override
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
			@Override
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

		fTabFolder.addSelectionListener(widgetSelectedAdapter(e -> updateTabSelection()));
		fTabFolder.setUnselectedImageVisible(false);

		fNotebook = toolkit.createComposite(container);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fNotebook.setLayoutData(gd);
		fNotebookLayout = new StackLayout();
		fNotebook.setLayout(fNotebookLayout);

		fFreeBSDSection = addFreeBSDSection(fNotebook, toolkit);
		fLinuxSection = addLinuxSection(fNotebook, toolkit);
		fMacSection = addMacSection(fNotebook, toolkit);
		fWin32Section = addWin32Section(fNotebook, toolkit);

		createTabs();

		toolkit.paintBordersFor(container);
		section.setClient(container);
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

	private void createTabs() {
		addTab("linux"); //$NON-NLS-1$
		addTab("macosx"); //$NON-NLS-1$
		addTab("win32"); //$NON-NLS-1$
		addTab("freebsd"); //$NON-NLS-1$

		String currentTarget = TargetPlatform.getOS();
		if (currentTarget == null) {
			currentTarget = "linux"; //$NON-NLS-1$
		}
		switch (currentTarget)
			{
		case "win32": //$NON-NLS-1$
			fTabFolder.setSelection(3);
			fNotebookLayout.topControl = fWin32Section;
			break;
		case "macosx": //$NON-NLS-1$
			fTabFolder.setSelection(1);
			fNotebookLayout.topControl = fMacSection;
			break;
		case "freebsd": //$NON-NLS-1$
			fTabFolder.setSelection(0);
			fNotebookLayout.topControl = fFreeBSDSection;
			break;
		default:
			fTabFolder.setSelection(0);
			fNotebookLayout.topControl = fLinuxSection;
			break;
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
			BidiUtils.applyBidiProcessing(ientry.getText(), StructuredTextTypeHandlerFactory.FILE);
			final int index = i;
			// Create validator
			fMultipleWinIconValidator[index] = new TextValidator(getManagedForm(), ientry.getText(), getProject(), true) {
				@Override
				protected boolean validateControl() {
					return validateMultipleWinIcon(ientry, index);
				}
			};
			// Disable initially
			fMultipleWinIconValidator[index].setEnabled(false);
			// Validate on modify
			ientry.getText().addModifyListener(e -> fMultipleWinIconValidator[index].validate());

			fIcons.add(ientry);
		}

		fIcoButton = toolkit.createButton(comp, PDEUIMessages.LauncherSection_ico, SWT.RADIO);
		td = new TableWrapData();
		td.colspan = 3;
		fIcoButton.setLayoutData(td);
		fIcoButton.addSelectionListener(widgetSelectedAdapter(e -> {
			boolean selected = fIcoButton.getSelection();
			getLauncherInfo().setUseWinIcoFile(selected);
			updateWinEntries(selected);
		}));
		fIcoButton.setEnabled(isEditable());

		final IconEntry ientry = new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_file, ILauncherInfo.P_ICO_PATH);
		// Create validator
		fSingleWinIconValidator = new TextValidator(getManagedForm(), ientry.getText(), getProject(), true) {
			@Override
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

	private Composite addFreeBSDSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_freebsdLabel, 3);
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_icon, ILauncherInfo.FREEBSD_ICON));
		toolkit.paintBordersFor(comp);
		return comp;
	}

	private Composite addLinuxSection(Composite parent, FormToolkit toolkit) {
		Composite comp = createComposite(parent, toolkit);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_linuxLabel, 3);
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_icon, ILauncherInfo.LINUX_ICON));
		toolkit.paintBordersFor(comp);
		return comp;
	}

	private Composite addMacSection(Composite parent, FormToolkit toolkit) {
		Composite compOuter = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(compOuter);
		Composite comp = createComposite(compOuter, toolkit);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(comp);
		createLabel(comp, toolkit, PDEUIMessages.LauncherSection_macLabel, 3);
		fIcons.add(new IconEntry(comp, toolkit, PDEUIMessages.LauncherSection_file, ILauncherInfo.MACOSX_ICON));
		fMacBundleUrlTypesComposite = new MacBundleUrlTypesComposite(compOuter, toolkit);
		toolkit.paintBordersFor(comp);
		return compOuter;
	}

	private class MacBundleUrlTypesComposite {

		private MacBundleUrlTypesPartAdapter fTablePart;
		private TableViewer fBundleUrlTypesTable;

		private class LabelProvider extends PDELabelProvider {
			@Override
			public Image getColumnImage(Object obj, int index) {
				return null;
			}

			@Override
			public String getColumnText(Object obj, int index) {
				IMacBundleUrlType bundleUrlType = (IMacBundleUrlType) obj;
				return switch (index) {
					case 0 -> bundleUrlType.getScheme();
					case 1 -> bundleUrlType.getName();
					default -> null;
				};
			}
		}

		public MacBundleUrlTypesComposite(Composite comp, FormToolkit toolkit) {
			createLabel(comp, toolkit,
					PDEUIMessages.LauncherSection_macBundleUrlTypes_LauncherSection_macBundleUrlTypesTitle, 2);
			fTablePart = new MacBundleUrlTypesPartAdapter(
					new String[] { PDEUIMessages.LauncherSection_macBundleUrlTypes_Add,
							PDEUIMessages.LauncherSection_macBundleUrlTypes_Edit,
							PDEUIMessages.LauncherSection_macBundleUrlTypes_Remove });
			fTablePart.createControl(comp, SWT.FULL_SELECTION, 1, toolkit);
			fBundleUrlTypesTable = (TableViewer) fTablePart.getViewer();
			fBundleUrlTypesTable.setComparator(new ViewerComparator());
			fBundleUrlTypesTable.addDoubleClickListener(event -> handleEdit());
			fBundleUrlTypesTable.getTable().addKeyListener(KeyListener.keyReleasedAdapter(e -> {
				if (e.keyCode == SWT.DEL) {
					handleRemove();
				}
			}));

			Table table = fBundleUrlTypesTable.getTable();
			TableColumn schemeColumn = new TableColumn(table, SWT.LEFT);
			schemeColumn.setText(PDEUIMessages.LauncherSection_macBundleUrlTypes_SchemeColumn);
			schemeColumn.setWidth(160);

			TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
			nameColumn.setText(PDEUIMessages.LauncherSection_macBundleUrlTypes_NameColumn);
			nameColumn.setWidth(220);

			table.addControlListener(ControlListener.controlResizedAdapter(e -> {
				int size = table.getSize().x;
				schemeColumn.setWidth(size / 10 * 4);
				nameColumn.setWidth(size / 10 * 6);
			}));

			table.setHeaderVisible(true);
			fBundleUrlTypesTable.setLabelProvider(new LabelProvider());
			fBundleUrlTypesTable.setContentProvider((IStructuredContentProvider) inputElement -> {
				if (inputElement instanceof IProduct product) {
					ILauncherInfo launcherInfo = product.getLauncherInfo();
					if (launcherInfo != null) {
						return launcherInfo.getMacBundleUrlTypes().toArray();
					}
				}
				return new Object[0];
			});
			fBundleUrlTypesTable.setInput(getProduct());

			updateButtons();
		}

		private class MacBundleUrlTypesPartAdapter extends TablePart {

			public MacBundleUrlTypesPartAdapter(String[] buttonLabels) {
				super(buttonLabels);
			}

			@Override
			public void selectionChanged(IStructuredSelection selection) {
				getManagedForm().fireSelectionChanged(LauncherSection.this, selection);
				updateButtons();
			}

			@Override
			public void buttonSelected(Button button, int index) {
				switch (index) {
					case 0 -> handleAdd();
					case 1 -> handleEdit();
					case 2 -> handleRemove();
				}
			}
		}

		private void handleAdd() {
			openBundleURLTypeDialog(null, getExistingBundleUrlTypes());
		}

		private void handleEdit() {
			IStructuredSelection ssel = fBundleUrlTypesTable.getStructuredSelection();
			if (!ssel.isEmpty() && ssel.getFirstElement() instanceof IMacBundleUrlType propertyToEdit) {
				Set<IMacBundleUrlType> existing = getExistingBundleUrlTypes();
				existing.remove(propertyToEdit);
				openBundleURLTypeDialog(propertyToEdit, existing);
			}
		}

		private void openBundleURLTypeDialog(IMacBundleUrlType propertyToEdit, Set<IMacBundleUrlType> existing) {
			BundleUrlTypeDialog dialog = new BundleUrlTypeDialog(PDEPlugin.getActiveWorkbenchShell(), propertyToEdit,
					existing);
			if (dialog.open() == Window.OK) {
				IMacBundleUrlType result = dialog.getResult();
				if (result != null) {
					fBundleUrlTypesTable.refresh();
					fBundleUrlTypesTable.setSelection(new StructuredSelection(result));
					updateButtons();
				}
			}
		}

		private Set<IMacBundleUrlType> getExistingBundleUrlTypes() {
			return new HashSet<>(getLauncherInfo().getMacBundleUrlTypes());
		}

		private void handleRemove() {
			IStructuredSelection ssel = fBundleUrlTypesTable.getStructuredSelection();
			if (!ssel.isEmpty()) {
				List<IMacBundleUrlType> bundleUrlTypes = ssel.toList();
				getLauncherInfo().removeMacBundleUrlTypes(bundleUrlTypes);
				fBundleUrlTypesTable.refresh(false);
			}
		}

		private void updateButtons() {
			ISelection selection = fBundleUrlTypesTable.getStructuredSelection();
			fTablePart.setButtonEnabled(0, isEditable());
			fTablePart.setButtonEnabled(1, isEditable() && !selection.isEmpty());
			fTablePart.setButtonEnabled(2, isEditable() && !selection.isEmpty());
		}

		private class BundleUrlTypeDialog extends StatusDialog {

			private Text fScheme;
			private Text fName;
			private IMacBundleUrlType fEdit;
			private final Set<IMacBundleUrlType> fExistingBundleUrlTypes;

			public BundleUrlTypeDialog(Shell shell, IMacBundleUrlType bundleUrlType,
					Set<IMacBundleUrlType> existingBundleUrlTypes) {
				super(shell);
				fEdit = bundleUrlType;
				fExistingBundleUrlTypes = existingBundleUrlTypes;
				setTitle(PDEUIMessages.LauncherSection_macBundleUrlTypes_DialogTitle);
			}

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite comp = (Composite) super.createDialogArea(parent);
				((GridLayout) comp.getLayout()).numColumns = 2;
				SWTFactory.createLabel(comp, PDEUIMessages.LauncherSection_macBundleUrlTypes_Scheme, 1);
				fScheme = SWTFactory.createSingleText(comp, 1);
				fScheme.addModifyListener(e -> validate());
				SWTFactory.createLabel(comp, PDEUIMessages.LauncherSection_macBundleUrlTypes_Name, 1);
				fName = SWTFactory.createSingleText(comp, 1);
				fName.addModifyListener(e -> validate());

				if (fEdit != null) {
					if (fEdit.getScheme() != null) {
						fScheme.setText(fEdit.getScheme());
					}
					if (fEdit.getName() != null) {
						fName.setText(fEdit.getName());
					}
				}
				// Disable ok button on startup
				updateStatus(Status.error("")); //$NON-NLS-1$
				return comp;
			}

			protected void validate() {
				String scheme = fScheme.getText().trim();
				String name = fName.getText().trim();
				if (scheme.length() == 0) {
					updateStatus(Status.error(PDEUIMessages.LauncherSection_macBundleUrlTypes_ErrorNoScheme));
				} else if (name.length() == 0) {
					updateStatus(Status.error(PDEUIMessages.LauncherSection_macBundleUrlTypes_ErrorNoName));
				} else if (fExistingBundleUrlTypes.stream().map(IMacBundleUrlType::getScheme)
						.anyMatch(scheme::equals)) {
					updateStatus(Status.error(
							NLS.bind(PDEUIMessages.LauncherSection_macBundleUrlTypes_ErrorSchemeExists, scheme)));
				} else {
					updateStatus(Status.OK_STATUS);
				}
			}

			@Override
			protected void okPressed() {
				if (fEdit != null) {
					getLauncherInfo().removeMacBundleUrlTypes(List.of(fEdit));
				}
				IProductModelFactory factory = getModel().getFactory();
				fEdit = factory.createMacBundleUrlType();
				fEdit.setScheme(fScheme.getText().trim());
				fEdit.setName(fName.getText().trim());
				getLauncherInfo().addMacBundleUrlTypes(List.of(fEdit));
				super.okPressed();
			}

			@Override
			protected Control createHelpControl(Composite parent) {
				return parent;
			}

			public IMacBundleUrlType getResult() {
				return fEdit;
			}
		}
	}

	private Composite createComposite(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = layout.topMargin = layout.leftMargin = layout.rightMargin = 0;
		layout.numColumns = 3;
		comp.setLayout(layout);
		return comp;
	}

	@Override
	public void refresh() {
		ILauncherInfo info = getLauncherInfo();
		fNameEntry.setValue(info.getLauncherName(), true);
		boolean useIco = info.usesWinIcoFile();
		fIcoButton.setSelection(useIco);
		fBmpButton.setSelection(!useIco);

		// Turn off auto message update until after values are set
		fSingleWinIconValidator.setRefresh(false);
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = fIcons.get(i);
			entry.setValue(info.getIconPath(entry.getIconId()), true);
		}
		// Turn back on auto message update
		fSingleWinIconValidator.setRefresh(true);

		updateWinEntries(useIco);

		fMacBundleUrlTypesComposite.fBundleUrlTypesTable.setInput(getProduct());
		fMacBundleUrlTypesComposite.fBundleUrlTypesTable.refresh();
		fMacBundleUrlTypesComposite.updateButtons();

		super.refresh();
	}

	private void updateWinEntries(boolean useIco) {
		for (int i = 0; i < fIcons.size(); i++) {
			IconEntry entry = fIcons.get(i);
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

	private void updateWinEntryValidators(boolean useIco) {
		// Turn off auto message update until after values are set
		fSingleWinIconValidator.setRefresh(false);
		// Update validator
		fSingleWinIconValidator.setEnabled(isEditable() && useIco);
		// Update validators
		for (TextValidator validator : fMultipleWinIconValidator) {
			validator.setEnabled(isEditable() && !useIco);
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

	@Override
	public void commit(boolean onSave) {
		fNameEntry.commit();
		for (int i = 0; i < fIcons.size(); i++) {
			fIcons.get(i).commit();
		}
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		for (int i = 0; i < fIcons.size(); i++) {
			fIcons.get(i).commit();
		}
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
		IPath path = IPath.fromOSString(value);
		if (path.isEmpty()) {
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); //
			return;
		}
		IResource resource = root.findMember(IPath.fromOSString(value));
		try {
			if (resource != null && resource instanceof IFile) {
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile) resource, true);
			} else {
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); //
			}
		} catch (PartInitException e) {
		}
	}

	private String getExtension(String iconId) {
		if (iconId.equals(ILauncherInfo.FREEBSD_ICON)) {
			return "xpm"; //$NON-NLS-1$
		}
		if (iconId.equals(ILauncherInfo.LINUX_ICON)) {
			return "xpm"; //$NON-NLS-1$
		}
		if (iconId.equals(ILauncherInfo.MACOSX_ICON)) {
			return "icns"; //$NON-NLS-1$
		}
		if (iconId.equals(ILauncherInfo.P_ICO_PATH)) {
			return "ico"; //$NON-NLS-1$
		}
		return "bmp"; //$NON-NLS-1$
	}

	@Override
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
				fNotebookLayout.topControl = fWin32Section;
				break;
			case 3 :
				fNotebookLayout.topControl = fFreeBSDSection;
				break;
		}
		if (oldPage != fNotebookLayout.topControl) {
			fNotebook.layout();
		}
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
