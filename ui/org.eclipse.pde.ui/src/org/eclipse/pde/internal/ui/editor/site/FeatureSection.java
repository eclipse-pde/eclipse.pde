/*
 * Created on Sep 29, 2003
 */
package org.eclipse.pde.internal.ui.editor.site;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author melhem
 */
public class FeatureSection extends PDEFormSection {
	
	private Composite fContainer;
	private TableViewer fFeaturesViewer;
	private boolean fUpdateNeeded;
	private ISiteModel fModel;
	private ISiteBuildModel fBuildModel;
	
	private Button fExposeButton;
	private Button fPatchButton;
	private FormEntry fURLEntry;
	private FormEntry fTypeEntry;
	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fLabelEntry;
	private FormEntry fOsEntry;
	private FormEntry fWsEntry;
	private FormEntry fArchEntry;
	private FormEntry fNlEntry;
	private TablePart fFeaturesTablePart;
	private CheckboxTablePart fCategoryTablePart;
	private CheckboxTableViewer fCategoryViewer;
	
	
	class FeatureContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		/*
		 * (non-Javadoc) @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			ISiteBuildModel model = (ISiteBuildModel)inputElement;
			return model.getSiteBuild().getFeatures();
		}
	}
	
	class CategoryContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		/*
		 * (non-Javadoc) @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			ISite model = (ISite)inputElement;
			return model.getCategoryDefinitions();
		}
	}
	
	/**
	 * @param formPage
	 */
	public FeatureSection(PDEFormPage formPage) {
		super(formPage);
		setHeaderText(PDEPlugin.getResourceString("SiteEditor.FeatureSection.header")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("SiteEditor.FeatureSection.desc")); //$NON-NLS-1$
		fModel = (ISiteModel) getFormPage().getModel();
		fModel.addModelChangedListener(this);
		fBuildModel = fModel.getBuildModel();
		fBuildModel.addModelChangedListener(this);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#dispose()
	 */
	public void dispose() {
		super.dispose();
		fModel.removeModelChangedListener(this);
		fBuildModel.removeModelChangedListener(this);
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#createClient(org.eclipse.swt.widgets.Composite, org.eclipse.update.ui.forms.internal.FormWidgetFactory)
	 */
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createLeftContainer(container, factory);
		createRightContainer(container, factory);
		handleFeatureSelectionChanged();
		factory.paintBordersFor(container);
		return container;
	}

	/**
	 * @param container
	 * @param factory
	 */
	private void createRightContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_BOTH));
		
		fExposeButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.FeatureSection.expose"), SWT.CHECK); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 1;
		fExposeButton.setLayoutData(gd);
		
		fExposeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExpose(fExposeButton.getSelection());
			}
		});
		createPropertiesSection(container, factory);
		factory.paintBordersFor(container);

	}
	
	private void handleExpose(boolean expose) {
		try {
			fContainer.setVisible(fExposeButton.getSelection());
			IStructuredSelection ssel = (IStructuredSelection)fFeaturesViewer.getSelection();
			ISiteBuildFeature sbFeature = (ISiteBuildFeature)ssel.getFirstElement();
			if (expose) {
				ISiteFeature feature = createSiteFeature(sbFeature);
				fModel.getSite().addFeatures(new ISiteFeature[] {feature});
				resetProperties(feature);
			} else {
				ISiteFeature feature = findMatchingSiteFeature(sbFeature);
				if (feature != null)
					fModel.getSite().removeFeatures(new ISiteFeature[]{feature});
			}
			forceDirty();
		} catch (CoreException e) {
		}
	}
	
	private ISiteFeature getSelectedFeatureRef() {
		IStructuredSelection ssel = (IStructuredSelection)fFeaturesViewer.getSelection();
		return findMatchingSiteFeature((ISiteBuildFeature)ssel.getFirstElement());
	}
	
	
	/**
	 * @param container
	 * @param factory
	 */
	private void createLeftContainer(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		fFeaturesTablePart = new TablePart(new String[] { PDEPlugin.getResourceString("SiteEditor.add"), PDEPlugin.getResourceString("SiteEditor.remove") }) { //$NON-NLS-1$ //$NON-NLS-2$
			protected void buttonSelected(Button button, int index) {
				switch (index) {
					case 0 :
						handleNewFeature();
						break;
					case 1 :
						handleRemoveFeature();
				}
			}
			protected void selectionChanged(IStructuredSelection selection) {
				handleFeatureSelectionChanged(selection);
			}
		};
		fFeaturesTablePart.createControl(container, SWT.MULTI, 2, factory);
		createContextMenu(fFeaturesTablePart.getControl());
		fFeaturesViewer = fFeaturesTablePart.getTableViewer();
		fFeaturesViewer.setContentProvider(new FeatureContentProvider());
		fFeaturesViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fFeaturesViewer.setInput(fBuildModel);
		factory.paintBordersFor(container);		
	}
	
	private void createPropertiesSection(Composite parent, FormWidgetFactory factory) {
		fContainer = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 1;
		layout.marginHeight = 2;
		layout.verticalSpacing = 8;
		fContainer.setLayout(layout);
		fContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createCategorySection(factory);
		
		fPatchButton = factory.createButton(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.patch"), SWT.CHECK); //$NON-NLS-1$
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		fPatchButton.setLayoutData(gd);
		fPatchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					getSelectedFeatureRef().setIsPatch(fPatchButton.getSelection());
					forceDirty();
				} catch (CoreException e1) {
				}
			}
		});

		fURLEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.url"), factory, 2)); //$NON-NLS-1$
		fURLEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setURL(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});		
		
		fLabelEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.label"), factory, 2)); //$NON-NLS-1$
		fLabelEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setLabel(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fIdEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.id"), factory, 2)); //$NON-NLS-1$
		fIdEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setId(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fVersionEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.version"), factory, 2)); //$NON-NLS-1$
		fVersionEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setVersion(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fTypeEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.type"), factory, 2)); //$NON-NLS-1$
		fTypeEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setType(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fOsEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.os"), factory, 2)); //$NON-NLS-1$
		fOsEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setOS(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fWsEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.ws"), factory, 2)); //$NON-NLS-1$
		fWsEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setWS(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});

		fNlEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.nl"), factory, 2)); //$NON-NLS-1$
		fNlEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setNL(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		fArchEntry = new FormEntry(createText(fContainer, PDEPlugin.getResourceString("SiteEditor.FeatureSection.arch"), factory, 2)); //$NON-NLS-1$
		fArchEntry.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					getSelectedFeatureRef().setArch(text.getValue());
				} catch (CoreException e) {
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		
		factory.paintBordersFor(fContainer);				
	}
	
	private void createCategorySection(FormWidgetFactory factory) {
		Label label = factory.createLabel(fContainer,PDEPlugin.getResourceString("SiteEditor.FeatureSection.category")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		
		fCategoryTablePart =
			new CheckboxTablePart(new String[] { PDEPlugin.getResourceString("SiteEditor.add"), PDEPlugin.getResourceString("SiteEditor.edit"), PDEPlugin.getResourceString("SiteEditor.remove") }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			protected void buttonSelected(Button button, int index) {
				switch (index) {
					case 0:
						handleAddCategoryDefinition();
						break;
					case 1:
						handleEditCategoryDefinition();
						break;
					case 2:
						handleRemoveCategoryDefinition();
				}
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.parts.CheckboxTablePart#elementChecked(java.lang.Object, boolean)
			 */
			protected void elementChecked(Object element, boolean checked) {
				handleCheckStateChanged((ISiteCategoryDefinition)element, checked);
			}
			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.parts.CheckboxTablePart#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
			 */
			protected void selectionChanged(IStructuredSelection ssel) {
				setButtonEnabled(1, ssel != null && ssel.size() == 1);
				setButtonEnabled(2, ssel != null && ssel.size() > 0);
			}
		};
		fCategoryTablePart.createControl(fContainer, SWT.MULTI, 2, factory);
		createContextMenu(fCategoryTablePart.getControl());
		fCategoryTablePart.setButtonEnabled(1, false);
		fCategoryTablePart.setButtonEnabled(2, false);
		fCategoryViewer = fCategoryTablePart.getTableViewer();
		fCategoryViewer.setContentProvider(new CategoryContentProvider());
		fCategoryViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fCategoryViewer.setInput(fModel.getSite());
	}
	
	private void createContextMenu(Control control) {
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				mng.add(new Action(PDEPlugin.getResourceString("SiteEditor.remove")) { //$NON-NLS-1$
					public void run() {
						doGlobalAction(IWorkbenchActionConstants.DELETE);
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
	
	private void handleCheckStateChanged(ISiteCategoryDefinition def, boolean checked) {
		try {
			ISiteFeature feature = getSelectedFeatureRef();
			ISiteCategory[] cats = feature.getCategories();
			if (!checked) {
				for (int i = 0; i < cats.length; i++) {
					if (cats[i].getName().equals(def.getName())) {
						feature.removeCategories(new ISiteCategory[] { cats[i] });
						break;
					}
				}
			} else {
				ISiteCategory cat = fModel.getFactory().createCategory(feature);
				cat.setName(def.getName());
				feature.addCategories(new ISiteCategory[] { cat });
			}
			forceDirty();
		} catch (CoreException e) {
		}
	}
		
	private void handleEditCategoryDefinition() {
		IStructuredSelection ssel = (IStructuredSelection)fCategoryViewer.getSelection();
		if (ssel != null && ssel.size() == 1)
			showCategoryDialog((ISiteCategoryDefinition)ssel.getFirstElement());
	}

	private void handleAddCategoryDefinition() {
		showCategoryDialog(null);
	}
	
	public void handleNewFeature() {
		final Control control = fFeaturesViewer.getTable();
		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
			public void run() {
				BuiltFeaturesWizard wizard = new BuiltFeaturesWizard(fBuildModel);
				WizardDialog dialog = new WizardDialog(control.getShell(), wizard);
				if (dialog.open() == WizardDialog.OK) {
					forceDirty();
					fFeaturesViewer.getTable().setSelection(fFeaturesViewer.getTable().getItemCount()-1);
					handleFeatureSelectionChanged();
				}
				
			}
		});
	}
	
	
	private void handleFeatureSelectionChanged() {
		handleFeatureSelectionChanged((IStructuredSelection)fFeaturesViewer.getSelection());

	}
	private void handleFeatureSelectionChanged(IStructuredSelection ssel) {
		if (ssel != null) {
			fExposeButton.setVisible(ssel.size() == 1);
			fContainer.setVisible(fExposeButton.getSelection() && ssel.size() == 1);
			if (ssel.size() == 1) {
				ISiteBuildFeature sbFeature = (ISiteBuildFeature) ssel.getFirstElement();
				resetProperties(findMatchingSiteFeature(sbFeature));
			}			
		}
		fFeaturesTablePart.setButtonEnabled(1, ssel != null && ssel.size() > 0);
	}

	private void forceDirty() {
		setDirty(true);
		((IEditable)fModel).setDirty(true);
		getFormPage().getEditor().fireSaveNeeded();
	}

	public void update() {
		if (fUpdateNeeded) {
			fFeaturesViewer.setInput(fBuildModel);
			fCategoryViewer.setInput(fModel.getSite());
			handleFeatureSelectionChanged();
			fUpdateNeeded = false;
		}
	}

	public void modelChanged(IModelChangedEvent event) {
			fUpdateNeeded = true;
			update();
	}
	
	public void initialize(Object input) {
		update();
	}

	public void commitChanges(boolean onSave) {
		if (onSave
			&& fBuildModel instanceof WorkspaceSiteBuildModel
			&& ((WorkspaceSiteBuildModel) fBuildModel).isDirty()) {
			((WorkspaceSiteBuildModel) fBuildModel).save();
		}
	}

	private ISiteFeature createSiteFeature(ISiteBuildFeature sbfeature)
		throws CoreException {
		ISiteFeature sfeature = fModel.getFactory().createFeature();
		sfeature.setId(sbfeature.getId());
		sfeature.setVersion(sbfeature.getVersion());
		sfeature.setURL(sbfeature.getTargetURL());
		IFeature refFeature = sbfeature.getReferencedFeature();
		sfeature.setOS(refFeature.getOS());
		sfeature.setWS(refFeature.getWS());
		sfeature.setArch(refFeature.getArch());
		sfeature.setNL(refFeature.getNL());
		return sfeature;
	}

	private ISiteFeature findMatchingSiteFeature(ISiteBuildFeature sbfeature) {
		ISiteFeature[] sfeatures = fModel.getSite().getFeatures();
		return findMatchingSiteFeature(sbfeature, sfeatures);
	}

	private ISiteFeature findMatchingSiteFeature(
		ISiteBuildFeature sbfeature,
		ISiteFeature[] sfeatures) {
		for (int j = 0; j < sfeatures.length; j++) {
			ISiteFeature sfeature = sfeatures[j];
			if (matches(sfeature, sbfeature))
				return sfeature;
		}
		return null;
	}
	
	private boolean matches(
		ISiteFeature sfeature,
		ISiteBuildFeature sbfeature) {
		String targetURL = sbfeature.getTargetURL();
		String url = sfeature.getURL();
		return (
			url != null
				&& targetURL != null
				&& url.equalsIgnoreCase(targetURL));
	}
	
	private void resetProperties(ISiteFeature feature) {
		if (feature == null) {
			fExposeButton.setSelection(false);
			fContainer.setVisible(false);
		} else {
			fExposeButton.setSelection(true);
			fPatchButton.setSelection(feature.isPatch());
			ISiteCategory[] cats = feature.getCategories();
			ArrayList defs = new ArrayList();
			for (int i = 0; i < cats.length; i++) {
				defs.add(cats[i].getDefinition());
			}
			fCategoryViewer.setCheckedElements(defs.toArray());
			fContainer.setVisible(true);
			fLabelEntry.setValue(feature.getLabel(), true);
			fURLEntry.setValue(feature.getURL(), true);
			fIdEntry.setValue(feature.getId(), true);
			fVersionEntry.setValue(feature.getVersion(), true);
			fTypeEntry.setValue(feature.getType(), true);
			fOsEntry.setValue(feature.getOS(), true);
			fWsEntry.setValue(feature.getWS(), true);
			fArchEntry.setValue(feature.getArch(), true);
			fNlEntry.setValue(feature.getNL(), true);
		}
	}
	
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers =
			new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			if (fFeaturesTablePart.getControl().isFocusControl()) {
				return handleRemoveFeature();
			}
			if (fCategoryTablePart.getControl().isFocusControl()) {
				return handleRemoveCategoryDefinition();
			}
		}
		return false;
	}

	private boolean handleRemoveCategoryDefinition() {
		try {
			IStructuredSelection ssel = (IStructuredSelection)fCategoryViewer.getSelection();
			if (ssel != null && ssel.size() > 0) {
				ISiteCategoryDefinition[] defs =
					(ISiteCategoryDefinition[]) ssel.toList().toArray(
						new ISiteCategoryDefinition[ssel.size()]);
				fModel.getSite().removeCategoryDefinitions(defs);
				fCategoryViewer.refresh();
				forceDirty();
				return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean handleRemoveFeature() {
		try {
			IStructuredSelection ssel = (IStructuredSelection)fFeaturesViewer.getSelection();
			if (ssel != null && ssel.size() > 0) {
				ISiteBuildFeature[] sbFeatures =
					(ISiteBuildFeature[]) ssel.toList().toArray(
						new ISiteBuildFeature[ssel.size()]);
				for (int i = 0; i < sbFeatures.length; i++) {
					ISiteFeature feature = findMatchingSiteFeature(sbFeatures[i]);
					if (feature != null)
						fModel.getSite().removeFeatures(new ISiteFeature[] {feature});
				}
				fBuildModel.getSiteBuild().removeFeatures(sbFeatures);
				forceDirty();
				return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}
	
	private void showCategoryDialog(
		final ISiteCategoryDefinition def) {
		BusyIndicator.showWhile(fCategoryViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				NewCategoryDefinitionDialog dialog =
					new NewCategoryDefinitionDialog(
						fCategoryViewer.getControl().getShell(),
						fModel,
						def);
				dialog.create();
				if (dialog.open() == NewCategoryDefinitionDialog.OK) {
					forceDirty();
					fCategoryViewer.refresh();
				}
			}
		});
	}


}
