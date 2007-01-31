/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class JRESection extends PDESection {

	private FormEntry fPath;
	private Button fBrowseJREsButton;
	private Button fBrowseFileSystemButton;
	private Button fBrowseVariablesButton;

	private static final String[] TAB_LABELS = new String[4];
	static {
		TAB_LABELS[IJREInfo.LINUX] = "linux"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.MACOS] = "macosx"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.SOLAR] = "solaris"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.WIN32] = "win32"; //$NON-NLS-1$
	}

	private CTabFolder fTabFolder;
	private int fLastTab;

	public JRESection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ProductJRESection_title); 
		section.setDescription(PDEUIMessages.ProductJRESection_desc); 

		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 5));

		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		gd.horizontalSpan = 5;
		gd.grabExcessHorizontalSpace = true;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor,
				toolkit.getColors().getBackground() },
				new int[] { 100 }, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {	
				refresh();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		fPath = new FormEntry(client, toolkit, PDEUIMessages.ProductJRESection_location, null, false); 
		fPath.getText().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPath.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getJVMLocations().setJVM(entry.getValue().trim(), fLastTab);
			}
		});

		fBrowseJREsButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_browseJREs, SWT.PUSH);
		fBrowseJREsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseJREs();
			}
		});

		fBrowseFileSystemButton = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_fileSystem, SWT.PUSH);
		fBrowseFileSystemButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFileSystem();
			}
		});

		fBrowseVariablesButton = toolkit.createButton(client, PDEUIMessages.TargetDefinitionSection_variables, SWT.PUSH);
		fBrowseVariablesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInsertVariable();
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
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_PLUGIN_CONFIG_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);

		String currentTarget = TargetPlatform.getOS();

		if (Platform.OS_WIN32.equals(currentTarget)) {
			fTabFolder.setSelection(3);
		} else if (Platform.OS_MACOSX.equals(currentTarget)) {
			fTabFolder.setSelection(1);
		} else if (Platform.OS_SOLARIS.equals(currentTarget)) {
			fTabFolder.setSelection(2);
		} 
	}

	public void refresh() {
		fLastTab = fTabFolder.getSelectionIndex();
		fPath.setValue(getJVMLocations().getJVM(fLastTab), true);
		super.refresh();
	}

	private IJREInfo getJVMLocations() {
		IJREInfo info = getProduct().getJVMLocations();
		if (info == null) {
			info = getModel().getFactory().createJVMInfo();
			getProduct().setJVMLocations(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}

	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(getSection().getShell());
		String path = fPath.getValue();
		if(path == null || path.length() == 0)
			path = VMHelper.getDefaultVMInstallLocation();

		dialog.setFilterPath(path);
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null) {
			fPath.setValue(result);
			getJVMLocations().setJVM(result, fLastTab);
		}
	}

	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
			new StringVariableSelectionDialog(getSection().getShell());
		if (dialog.open() == Window.OK) {
			fPath.getText().insert(dialog.getVariableExpression());
			// have to setValue to make sure getValue reflects the actual text in the Text object.
			fPath.setValue(fPath.getText().getText());
			getJVMLocations().setJVM(fPath.getText().getText(), fLastTab);
		}
	}

	protected void handleBrowseJREs() {
		ListDialog dialog = new ListDialog(getSection().getShell());
		dialog.setContentProvider(new IStructuredContentProvider() {

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			public void dispose() {}

			public Object[] getElements(Object inputElement) {
				return VMHelper.getAllVMInstances();
			}

		});
		dialog.setLabelProvider(new LabelProvider() {

			public String getText(Object element) {
				IVMInstall vm = (IVMInstall) element;
				return vm.getName() + " (" + vm.getInstallLocation().getAbsolutePath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			public Image getImage(Object element) {
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
			}

		});
		dialog.setInput(this);
		dialog.setTitle(PDEUIMessages.ProductJRESection_selectJREsTitle);
		dialog.setMessage(PDEUIMessages.ProductJRESection_selectJREsMessage);
		if (dialog.open() == Window.OK) {
			IVMInstall vm = (IVMInstall) dialog.getResult()[0];
			fPath.setValue(vm.getInstallLocation().getAbsolutePath());
			getJVMLocations().setJVM(vm.getInstallLocation().getAbsolutePath(), fLastTab);
		}
	}

}
