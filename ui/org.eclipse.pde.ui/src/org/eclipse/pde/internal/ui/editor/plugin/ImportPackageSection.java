/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;

import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.*;

public class ImportPackageSection extends TableSection implements IModelChangedListener {
	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	private TableViewer importPackageTable;

	private Vector importPackages;

	class ImportPackageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (importPackages == null) {
				createImportObjects();
			}
			return importPackages.toArray();
		}

		private void createImportObjects() {
			importPackages = new Vector();

			BundlePluginBase base = getPluginBase();
			if (base != null) {
				IBundle bundle = base.getBundle();
				if (bundle != null) {
					try {
						String value = bundle
								.getHeader(Constants.IMPORT_PACKAGE);
						if (value != null && !(value.trim()).equals("")) { //$NON-NLS-1$
							ManifestElement[] elements = ManifestElement
									.parseHeader(Constants.IMPORT_PACKAGE,
											value);
							for (int i = 0; i < elements.length; i++) {
								PackageObject p = new PackageObject(
										elements[i].getValue(),
										elements[i]
												.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION));
								importPackages.add(p);
							}
						}
					} catch (BundleException e) {
					}
				}
			}
		}
	}

	class ImportPackageLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	class ImportPackageDialogLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}

		public String getText(Object element) {
			ImportPackageSpecification p = (ImportPackageSpecification) element;
			return p.getName() + "(" + p.getVersionRange() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public ImportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {"Add...", "Remove"}); 
		getSection().setText("Required Packages"); 
		getSection()
				.setDescription("You can specify packages this plug-in depends on without explicitly restricting what plug-ins they must come from."); 
		getTablePart().setEditable(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		TablePart tablePart = getTablePart();
		importPackageTable = tablePart.getTableViewer();
		importPackageTable
				.setContentProvider(new ImportPackageContentProvider());
		importPackageTable.setLabelProvider(new ImportPackageLabelProvider());
		importPackageTable.setSorter(new ViewerSorter());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		initialize();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
		getTablePart().setButtonEnabled(2, item != null);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}

	/**
	 * 
	 */
	protected void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) importPackageTable
				.getSelection();
		Object[] items = ssel.toArray();
		importPackageTable.remove(items);
		removeImportPackages(items);

	}

	/**
	 * @param items
	 */
	private void removeImportPackages(Object[] removed) {
		for (int k = 0; k < removed.length; k++) {
			PackageObject p = (PackageObject) removed[k];
			for (int i = 0; i < importPackages.size(); i++) {
				PackageObject element = (PackageObject) importPackages.get(i);
				String name = element.getName();
				if (name.equals(p.getName())) {
					importPackages.remove(i);
					break;
				}
			}
		}
		writeImportPackages();
	}

	/**
	 * 
	 */
	protected void handleAdd() {
		ImportPackageSelectionDialog dialog = new ImportPackageSelectionDialog(
				importPackageTable.getTable().getShell(),
				new ImportPackageDialogLabelProvider(), getAvailablePackages());
		dialog.create();
		if (dialog.open() == ImportPackageSelectionDialog.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				ImportPackageSpecification candidate = (ImportPackageSpecification) models[i];
				PackageObject p = new PackageObject(candidate.getName(), null);
				importPackages.add(p);
				importPackageTable.add(p);
			}
			if (models.length > 0) {
				writeImportPackages();
			}
		}
	}

	public void addImportPackage(PackageObject p) {
		importPackages.add(p);
		importPackageTable.add(p);
		writeImportPackages();
	}

	private BundlePluginBase getPluginBase() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase base = model.getPluginBase();
		if (base instanceof BundlePluginBase) {
			return (BundlePluginBase) model.getPluginBase();
		}
		return null;
	}

	/**
	 * @param pluginBase
	 * @param importNode
	 */
	private void writeImportPackages() {
		BundlePluginBase base = getPluginBase();
		if (base == null)
			return;

		IBundle bundle = base.getBundle();
		if (bundle != null) {
			StringBuffer buffer = new StringBuffer();
			if (importPackages != null) {
				for (int i = 0; i < importPackages.size(); i++) {
					PackageObject iimport = (PackageObject) importPackages
							.get(i);
					buffer.append(iimport.getName());
					String version = iimport.getVersion();
					if (version != null && version.trim().length() > 0)
						buffer
								.append(";" + Constants.PACKAGE_SPECIFICATION_VERSION + "=\"" + version.trim() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (i < importPackages.size() - 1) {
						buffer
								.append("," + System.getProperty("line.separator") + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
			bundle.setHeader(Constants.IMPORT_PACKAGE, buffer.toString());
		}
	}

	private Vector getAvailablePackages() {
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager()
				.getPlugins();

		Vector result = new Vector();
		for (int i = 0; i < plugins.length; i++) {
			if (!(plugins[i].getPluginBase().getId()).equals(getPluginBase()
					.getId())) {
				BundleDescription bd = plugins[i].getBundleDescription();
				if (bd != null) {
					ImportPackageSpecification[] elements = bd.getImportPackages();
					if (elements != null) {
						for (int j = 0; j < elements.length; j++) {
								if (!isExistingPackage(elements[j]))
									result.add(elements[j]);
						}
					}
				}
			}
		}
		return result;
	}

	private boolean isExistingPackage(ImportPackageSpecification ps) {
		if (importPackages != null) {
			for (int i = 0; i < importPackages.size(); i++) {
				PackageObject p = (PackageObject) importPackages.get(i);
				if (p.getName().equals(ps.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		importPackageTable.setInput(model.getPluginBase());

		if (isBundleMode()) {
			getBundleModel().addModelChangedListener(this);
			getTablePart().setButtonEnabled(0, true);
		} else {
			getTablePart().setButtonEnabled(0, false);
		}
		getTablePart().setButtonEnabled(1, false);
		getTablePart().setButtonEnabled(2, false);

	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
	}

	public void refresh() {
		importPackages = null;
		importPackageTable.refresh();
		super.refresh();
	}

	private boolean isBundleMode() {
		return getPage().getModel() instanceof IBundlePluginModelBase;
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}

	IBundleModel getBundleModel() {
		InputContextManager contextManager = getPage().getPDEEditor()
				.getContextManager();
		if (contextManager == null)
			return null;
		InputContext context = contextManager
				.findContext(BundleInputContext.CONTEXT_ID);
		if (context != null)
			return (IBundleModel) context.getModel();
		return null;
	}

}
