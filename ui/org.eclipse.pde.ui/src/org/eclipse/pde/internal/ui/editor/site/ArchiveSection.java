/*
 * Created on Sep 29, 2003
 */
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.model.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author melhem
 */
public class ArchiveSection extends PDEFormSection {
	
	private Table fTable;
	private TableViewer fViewer;
	private boolean fUpdateNeeded;
	private ISiteModel fModel;
	private ISiteBuildModel fBuildModel;
	private Button fAddButton;	
	private Button fEditButton;
	private Button fRemoveButton;
	private FormEntry fPluginDest;
	private FormEntry fFeatureDest;
	
	class FolderProvider extends WorkbenchContentProvider {
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IFolder) {
					return true;
				}
			}
			return false;
		}
		
	}
	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			ISiteModel model = (ISiteModel)parent;
			return model.getSite().getArchives();
		}
	}

	class ArchiveLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ISiteArchive archive = (ISiteArchive)obj;
			switch (index) {
				case 0:
					return archive.getPath();
				case 1:
					return archive.getURL();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}
	/**
	 * @param formPage
	 */
	public ArchiveSection(PDEFormPage formPage) {
		super(formPage);
		setHeaderText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.header"));
		setDescription(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.title"));
		fModel = (ISiteModel)getFormPage().getModel();
		fModel.addModelChangedListener(this);
		fBuildModel = fModel.getBuildModel();
		if (fBuildModel != null)
			fBuildModel.addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#createClient(org.eclipse.swt.widgets.Composite, org.eclipse.update.ui.forms.internal.FormWidgetFactory)
	 */
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 9;
		layout.horizontalSpacing = 9;
		container.setLayout(layout);
		
		createTopContainer(container, factory);
		Label label = factory.createLabel(container, PDEPlugin.getResourceString("SiteEditor.Archive.instruction"), SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		label.setLayoutData(gd);
		
		createBottomContainer(container, factory);
		factory.paintBordersFor(container);		
		return container;
	}
	
	private void createTopContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fPluginDest =
		new FormEntry(
			createText(
				container,
				PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.pluginLocation")), //$NON-NLS-1$
				factory,
				1));
		fPluginDest.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setPluginDestination(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		Button browse = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.DescriptionSection.browse"), SWT.PUSH); //$NON-NLS-1$
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IFolder folder = handleFindContainer();
				if (folder != null)
					fPluginDest.setValue(folder.getProjectRelativePath().addTrailingSeparator().toString());
			}
		});
		
		fFeatureDest =
			new FormEntry(
				createText(
					container,
					PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.featureLocation")), //$NON-NLS-1$
					factory,
					1));
		fFeatureDest.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				setFeatureDestination(text.getValue());
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
	
		browse = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.DescriptionSection.browse"), SWT.PUSH); //$NON-NLS-1$
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IFolder folder = handleFindContainer();
				if (folder != null)
					fFeatureDest.setValue(folder.getProjectRelativePath().addTrailingSeparator().toString());
			}
		});	
		factory.paintBordersFor(container);
	}
	
	private void createBottomContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createTable(container, factory);
		createTableViewer();
		createButtons(container, factory);	
		factory.paintBordersFor(container);
	}
	
	private void createButtons(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		
		fAddButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.add"), SWT.PUSH);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showDialog(null);
			}
		});
		
		fEditButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.edit"), SWT.PUSH);
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)fViewer.getSelection();
				if (ssel != null && ssel.size() == 1)
					showDialog((ISiteArchive)ssel.getFirstElement());
			}
		});
		
		
		fRemoveButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.remove"), SWT.PUSH);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});
		fRemoveButton.setEnabled(false);
		fEditButton.setEnabled(false);
		factory.paintBordersFor(container);
	}
	
	private void createTable(Composite container, FormWidgetFactory factory) {
		fTable = factory.createTable(container, SWT.FULL_SELECTION|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		fTable.setLayoutData(gd);
		
		TableColumn col1 = new TableColumn(fTable, SWT.NULL);
		col1.setText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.col1"));
		
		TableColumn col2 = new TableColumn(fTable, SWT.NULL);
		col2.setText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.col2"));
		
		TableLayout tlayout = new TableLayout();
		tlayout.addColumnData(new ColumnWeightData(50, 200));
		tlayout.addColumnData(new ColumnWeightData(50, 200));
		fTable.setLayout(tlayout);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);
		
		createContextMenu(fTable);
	}
	
	private void createTableViewer() {
		fViewer = new TableViewer(fTable);
		fViewer.setContentProvider(new ContentProvider());
		fViewer.setLabelProvider(new ArchiveLabelProvider());
		fViewer.setInput(getFormPage().getModel());
		
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged();				
			}});
	}
	
	private void handleSelectionChanged() {
		ISelection selection = fViewer.getSelection();
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			fRemoveButton.setEnabled(ssel.size() > 0);
			fEditButton.setEnabled(ssel.size() == 1);
		} else {
			fRemoveButton.setEnabled(false);
			fEditButton.setEnabled(false);
		}
	}
	
	private void showDialog(final ISiteArchive archive) {
		final ISiteModel model = (ISiteModel) getFormPage().getModel();
		BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {
			public void run() {
				NewArchiveDialog dialog =
					new NewArchiveDialog(fTable.getShell(), model, archive);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, -1);
				if (dialog.open() == NewArchiveDialog.OK)
					fViewer.refresh();
			}
		});
	}
	
	private void handleDelete() {
		try {
			ISelection selection = fViewer.getSelection();
			if (selection != null && selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection)selection;
				if (ssel.size() > 0) {
					ISiteArchive[] array =
						(ISiteArchive[]) ssel.toList().toArray(new ISiteArchive[ssel.size()]);
					ISite site = ((ISiteModel) getFormPage().getModel()).getSite();
					site.removeArchives(array);
					forceDirty();
					fViewer.refresh();
				}
			}
		} catch (CoreException e) {
		}				
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator
				.showWhile(fTable.getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		return false;
	}

	public void update() {
		if (fUpdateNeeded) {
			update(getFormPage().getModel());
		}
		ISiteBuildModel buildModel = fModel.getBuildModel();
		if (buildModel != null) {
			ISiteBuild siteBuild = buildModel.getSiteBuild();
			setIfDefined(
				fFeatureDest,
				siteBuild.getFeatureLocation() != null
					? siteBuild.getFeatureLocation().toString()
					: null);
			setIfDefined(
				fPluginDest,
				siteBuild.getPluginLocation() != null
					? siteBuild.getPluginLocation().toString()
					: null);
		}
	}
	
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void update(Object input) {
		fViewer.refresh();		
		ISiteBuild siteBuild = fBuildModel.getSiteBuild();
		if (siteBuild != null) {
			setIfDefined(fPluginDest, siteBuild.getPluginLocation().toOSString());
			setIfDefined(fFeatureDest, siteBuild.getFeatureLocation().toOSString());
		}
		fUpdateNeeded = false;
	}
	
	public void initialize(Object input) {
		update(input);
	}

	public void modelChanged(IModelChangedEvent e) {
		fUpdateNeeded = true;
		update();
	}
	
	private void forceDirty() {
		setDirty(true);
		ISiteModel model = (ISiteModel) getFormPage().getModel();

		if (model instanceof IEditable) {
			((IEditable) model).setDirty(true);
		}
		getFormPage().getEditor().fireSaveNeeded();
	}
	
	private void createContextMenu(Control control) {
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				mng.add(new Action(PDEPlugin.getResourceString("SiteEditor.remove")) {
					public void run() {
						doGlobalAction(ActionFactory.DELETE.getId());
					}
				});
				mng.add(new Separator());
				PDEEditorContributor contributor =
					getFormPage().getEditor().getContributor();
				contributor.contextMenuAboutToShow(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		control.setMenu(popupMenuManager.createContextMenu(control));
	}

	public void commitChanges(boolean onSave) {
		fPluginDest.commit();
		fFeatureDest.commit();
		if (onSave && fBuildModel instanceof WorkspaceSiteBuildModel
				&& ((WorkspaceSiteBuildModel) fBuildModel).isDirty()) {
			((WorkspaceSiteBuildModel) fBuildModel).save();
		}
	}
	
	private void setPluginDestination(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setPluginLocation(new Path(text));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private IFolder handleFindContainer() {
		FolderSelectionDialog dialog =
			new FolderSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(),
				new FolderProvider() {
		});
		dialog.setInput(PDEPlugin.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					IResource resource = ((ISiteModel)getFormPage().getModel()).getUnderlyingResource();
					if (resource != null)
					return ((IProject)element).equals(resource.getProject());
				}
				return element instanceof IFolder;
			}			
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("SiteEditor.DescriptionSection.folderSelection")); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
					&& selection.length > 0
					&& selection[0] instanceof IFolder)
					return new Status(
						IStatus.OK,
						PDEPlugin.getPluginId(),
						IStatus.OK,
						"", //$NON-NLS-1$
						null);
				return new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					"", //$NON-NLS-1$
					null);
			}
		});
		if (dialog.open() == FolderSelectionDialog.OK) {
			return (IFolder) dialog.getFirstResult();
		}
		return null;
	}

	private void setFeatureDestination(String text) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel == null)
			return;
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		try {
			siteBuild.setFeatureLocation(new Path(text));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

}
