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
import java.util.Vector;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.*;
/**
 *
 */
public class ExportSection extends TableSection
		implements
			IPartSelectionListener, IInputContextListener {
	public static final String SECTION_TITLE = "ManifestEditor.ExportSection.title"; //$NON-NLS-1$
	public static final String SECTION_DESC = "ManifestEditor.ExportSection.desc"; //$NON-NLS-1$
	public static final String KEY_NO_EXPORT = "ManifestEditor.ExportSection.noExport"; //$NON-NLS-1$
	public static final String KEY_NEW_FILTER = "ManifestEditor.ExportSection.newFilter"; //$NON-NLS-1$
	public static final String KEY_FULL_EXPORT = "ManifestEditor.ExportSection.fullExport"; //$NON-NLS-1$
	public static final String KEY_SELECTED_EXPORT = "ManifestEditor.ExportSection.selectedExport"; //$NON-NLS-1$
	public static final String KEY_ADD = "ManifestEditor.ExportSection.add"; //$NON-NLS-1$
	public static final String KEY_REMOVE = "ManifestEditor.ExportSection.remove"; //$NON-NLS-1$
	public static final String SECTION_ADD_TITLE = "ManifestEditor.ExportSection.addTitle"; //$NON-NLS-1$
	private Button fFullExportButton;
	private Button fSelectedExportButton;
	private IPluginLibrary fCurrentLibrary;
	private Composite fPackageExportContainer;
	private TableViewer fPackageExportViewer;

	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IModel) {
				return getProvidedPackages();
			}
			else if (parent instanceof IPluginLibrary) {
				String[] filters = ((IPluginLibrary) parent)
						.getContentFilters();
				return filters == null ? new Object[0] : filters;
			}
			return new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}
	public ExportSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[]{
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(new GridLayout());
		String label = PDEPlugin.getResourceString(KEY_FULL_EXPORT);
		fFullExportButton = toolkit.createButton(container, label, SWT.RADIO);
		fFullExportButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFullExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (fCurrentLibrary != null)
						fCurrentLibrary.setExported(fFullExportButton
								.getSelection());
					getTablePart().setButtonEnabled(0,
							!fFullExportButton.getSelection());
					getTablePart().setButtonEnabled(1, false);
				} catch (CoreException e1) {
				}
			}
		});
		label = PDEPlugin.getResourceString(KEY_SELECTED_EXPORT);
		fSelectedExportButton = toolkit.createButton(container, label,
				SWT.RADIO);
		fSelectedExportButton.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
		fPackageExportContainer = toolkit.createComposite(container);
		fPackageExportContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.numColumns = 2;
		fPackageExportContainer.setLayout(layout);
		createNameTable(fPackageExportContainer, toolkit);
		update(null, isBundleMode());
		initialize();
		section.setClient(container);
	}
	private void createNameTable(Composite parent, FormToolkit toolkit) {
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(getPage().getModel().isEditable());
		createViewerPartControl(parent, SWT.FULL_SELECTION, 2, toolkit);
		fPackageExportViewer = tablePart.getTableViewer();
		fPackageExportViewer.setContentProvider(new TableContentProvider());
		fPackageExportViewer.setLabelProvider(new TableLabelProvider());
		fPackageExportViewer.setSorter(new ViewerSorter());
		toolkit.paintBordersFor(parent);
	}
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
	}
	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		return false;
	}
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		InputContextManager contextManager = getPage().getPDEEditor().getContextManager();
		if (contextManager!=null)
			contextManager.removeInputContextListener(this);
		super.dispose();
	}
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}
	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				String[] names;
				if (isBundleMode())
					names = getProvidedPackages();
				else
					names = fCurrentLibrary.getContentFilters();
				Vector existing = new Vector();
				if (names != null) {
					for (int i = 0; i < names.length; i++) {
						existing.add(names[i]);
					}
				}
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				PackageSelectionDialog dialog = new PackageSelectionDialog(
						fPackageExportViewer.getTable().getShell(),
						labelProvider, JavaCore.create(project), existing);
				if (dialog.open() == PackageSelectionDialog.OK) {
					Object[] elements = dialog.getResult();
					if (isBundleMode())
						addProvidedPackages(elements);
					else {
						for (int i = 0; i < elements.length; i++) {
							IPackageFragment fragment = (IPackageFragment) elements[i];
							fCurrentLibrary.addContentFilter(fragment
								.getElementName());
						}
					}
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fPackageExportViewer
				.getSelection();
		Object[] items = ssel.toArray();
		try {
			if (isBundleMode())
				removeProvidedPackages(items);
			else {
			for (int i = 0; i < items.length; i++) {
				fCurrentLibrary.removeContentFilter(items[i].toString());
			}
			}
		} catch (CoreException e) {
		}
	}
	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		InputContextManager contextManager = getPage().getPDEEditor().getContextManager();
		if (contextManager!=null)
			contextManager.addInputContextListener(this);
		if (isBundleMode())
			getBundleModel().addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			if (fCurrentLibrary!=null)
				update(null, isBundleMode());
			markStale();
			return;
		}
		refresh();
	}

	public void refresh() {
		update(fCurrentLibrary, isBundleMode());		
		super.refresh();
	}

	public void selectionChanged(IFormPart source, ISelection selection) {
		if (selection == null || selection.isEmpty())
			update(null, isBundleMode());
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.getFirstElement() instanceof IPluginLibrary)
			update((IPluginLibrary) ssel.getFirstElement(), isBundleMode());
	}
	private boolean isReadOnly() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IEditable)
			return !((IEditable) model).isEditable();
		return true;
	}
	private boolean isBundleMode() {
		return getPage().getModel() instanceof IBundlePluginModelBase;
	}
	private void update(IPluginLibrary library, boolean bundleMode) {
		fCurrentLibrary = library;
		// Don't do anything else if we are in the bundle mode
		if (bundleMode) {
			updateInBundleMode();
			return;
		}
		if (library == null) {
			fFullExportButton.setEnabled(false);
			fFullExportButton.setSelection(false);
			fSelectedExportButton.setEnabled(false);
			fSelectedExportButton.setSelection(false);
			fPackageExportViewer.setInput(new Object[0]);
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
			return;
		}
		fFullExportButton.setEnabled(!isReadOnly());
		fSelectedExportButton.setEnabled(!isReadOnly());
		fFullExportButton.setSelection(library.isFullyExported());
		fSelectedExportButton.setSelection(!library.isFullyExported());
		fPackageExportViewer.setInput(library);
		getTablePart().setButtonEnabled(1, false);
		getTablePart()
				.setButtonEnabled(0, fSelectedExportButton.getSelection());
	}
	
	private void updateInBundleMode() {
		getTablePart().setButtonEnabled(1, false);
		getTablePart().setButtonEnabled(0, true);
		fFullExportButton.setEnabled(false);
		fFullExportButton.setSelection(false);
		fSelectedExportButton.setEnabled(false);
		fSelectedExportButton.setSelection(true);
		fPackageExportViewer.setInput(getPage().getModel());
	}
	
	private String[] getProvidedPackages() {
		IBundleModel model = getBundleModel();
		IBundle bundle = model.getBundle();
		if (bundle == null)
			return new String[0];
		String value = bundle.getHeader(Constants.PROVIDE_PACKAGE);
		if (value == null)
			return new String[0];
		try {
			ManifestElement [] result = ManifestElement.parseHeader(Constants.PROVIDE_PACKAGE, value);
			String [] names = new String[result.length];
			for (int i=0; i<result.length; i++) {
				names[i] = result[i].getValue();
			}
			return names;
		} catch (BundleException e) {
		}
		return new String[0];				
	}
	private void addProvidedPackages(Object [] names) {
		String [] current = getProvidedPackages();
		Object [] newNames;
		if (current.length==0)
			newNames = names;
		else
			newNames = new Object[current.length+names.length];
		System.arraycopy(current, 0, newNames, 0, current.length);
		System.arraycopy(names, 0, newNames, current.length, names.length);
		setProvidedPackages(newNames);
	}
	private void removeProvidedPackages(Object [] removed) {
		String [] current = getProvidedPackages();
		ArrayList result = new ArrayList();
		for (int i=0; i<current.length; i++) {
			String name = current[i];
			boolean skip=false;
			for (int j=0; j<removed.length; j++) {
				if (name.equals(removed[j])) {
					skip=true;
					break;
				}
			}
			if (skip) continue;
			result.add(name);
		}
		setProvidedPackages(result.toArray());
	}
	
	IBundleModel getBundleModel() {
		InputContextManager contextManager = getPage().getPDEEditor().getContextManager();
		if (contextManager==null) return null;
		InputContext context = contextManager.findContext(BundleInputContext.CONTEXT_ID);
		if (context!=null)
			return (IBundleModel)context.getModel();
		return null;
	}
	
	private void setProvidedPackages(Object [] names) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<names.length; i++) {
			String name;
			if (names[i] instanceof IPackageFragment)
				name = ((IPackageFragment)names[i]).getElementName();
			else
				name = names[i].toString();
			
			buf.append(name);
			if (i < names.length - 1)
				buf.append("," + System.getProperty("line.separator") + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		IBundleModel model = getBundleModel();
		IBundle bundle = model.getBundle();
		if (bundle == null) return;
		bundle.setHeader(Constants.PROVIDE_PACKAGE, buf.toString());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextAdded(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			bundleModeChanged((IBundleModel)context.getModel(), true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			bundleModeChanged((IBundleModel)context.getModel(), false);
	}
	private void bundleModeChanged(IBundleModel model, boolean added) {
		if (added) {
			update(fCurrentLibrary, true);
			model.addModelChangedListener(this);
		}
		else {
			model.removeModelChangedListener(this);
			update(fCurrentLibrary, false);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return false;
	}
}
