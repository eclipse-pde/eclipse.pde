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
package org.eclipse.pde.internal.ui.wizards.extension;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class PointSelectionPage
	extends BaseWizardSelectionPage {
	private TableViewer fPointListViewer;
	private TableViewer fTemplateViewer;
	private IPluginBase fPluginBase;
	private Button fFilterCheck;
	private IPluginExtensionPoint fCurrentPoint;
	private HashSet fAvailableImports;
	private Action showDetailsAction;
	private IProject project;
	private Label templateLabel;
	private ExtensionTreeSelectionPage wizardsPage;
	
	private IPluginExtension fNewExtension;
	private ShowDescriptionAction fShowDescriptionAction;
	private WizardCollectionElement templateCollection;
	private WizardCollectionElement wizardCollection;
	private NewExtensionWizard wizard;
	
	class PointFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!fFilterCheck.getSelection())
				return true;
			IPluginExtensionPoint point = (IPluginExtensionPoint) element;
			return fAvailableImports.contains(point.getPluginBase().getId());
		}
	}
	
	class TemplateContentProvider extends DefaultContentProvider implements IStructuredContentProvider{
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IPluginExtensionPoint){
				PluginExtensionPoint point = (PluginExtensionPoint)inputElement;
				ArrayList result = new ArrayList();
				if (templateCollection.getWizards() != null) {
					Object[] wizards = templateCollection.getWizards().getChildren();
					for (int i = 0; i<wizards.length; i++){
						String wizardContributorId = ((WizardElement)wizards[i]).getContributingId();
						if (wizardContributorId == null || point == null || point.getFullId() == null)
							continue;
						if (wizards[i] instanceof WizardElement && wizardContributorId.equals(point.getFullId()))
							result.add((WizardElement)wizards[i]);
					}
					return result.toArray();
				}
			}
			return new Object[0];
		}
		
	}
	
	class PointContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			HashSet extPoints = new HashSet();
			PluginModelManager manager = (PluginModelManager)parent;
			IPluginModelBase[] plugins = manager.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				IPluginExtensionPoint[] points = plugins[i].getPluginBase().getExtensionPoints();
				for (int j = 0; j < points.length; j++)
					extPoints.add(points[j]);
			}
			return extPoints.toArray();
		}
	}

	class PointLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public String getColumnText(Object obj, int index) {
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			if (provider.isFullNameModeEnabled())
				return provider.getText((IPluginExtensionPoint) obj);
			return ((IPluginExtensionPoint) obj).getFullId();
		}
		
		public Image getImage(Object obj) {
			return getColumnImage(obj, 0);
		}
		
		public Image getColumnImage(Object obj, int index) {
			IPluginExtensionPoint exp = (IPluginExtensionPoint) obj;
			int flag =
				fAvailableImports.contains(exp.getPluginBase().getId())
					? 0
					: SharedLabelProvider.F_WARNING;
			if (((TemplateContentProvider)fTemplateViewer.getContentProvider()).getElements(exp).length >0)
				return PDEPlugin.getDefault().getLabelProvider().get(
						PDEPluginImages.DESC_NEWEXP_WIZ_TOOL,
						flag);
			return PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_EXT_POINT_OBJ,
				flag);
		}
	}

	public PointSelectionPage(IProject project, IPluginBase model, WizardCollectionElement element, WizardCollectionElement templates, NewExtensionWizard wizard) {
		super("pointSelectionPage", PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.fPluginBase = model;
		this.wizardCollection = element;
		this.templateCollection = templates;
		this.wizard= wizard;
		this.project=project;
		fAvailableImports = PluginSelectionDialog.getExistingImports(model);
		setTitle(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.desc")); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		makeActions();
	}
	
	public void createControl(Composite parent) {
		// tab folder
		final TabFolder tabFolder = new TabFolder(parent, SWT.FLAT);
		TabItem firstTab = new TabItem(tabFolder, SWT.NULL);
		firstTab.setText(PDEPlugin.getResourceString("PointSelectionPage.tab1")); //$NON-NLS-1$
		TabItem secondTab = new TabItem(tabFolder, SWT.NULL);
		secondTab.setText(PDEPlugin.getResourceString("PointSelectionPage.tab2")); //$NON-NLS-1$
		secondTab.setControl(createWizardsPage(tabFolder));
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection(tabFolder.getSelectionIndex());
			}
		});
		// top level group
		Composite outerContainer = new Composite(tabFolder, SWT.NONE);
		firstTab.setControl(outerContainer);
		GridLayout layout = new GridLayout();
		outerContainer.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		outerContainer.setLayoutData(gd);

		Composite pointContainer = new Composite(outerContainer, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		pointContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		pointContainer.setLayoutData(gd);

		Label pointLabel = new Label(pointContainer, SWT.NONE);
		pointLabel.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.availExtPoints.label")); //$NON-NLS-1$
		
		fPointListViewer =
			new TableViewer(
				pointContainer,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fPointListViewer.setContentProvider(new PointContentProvider());
		fPointListViewer.setLabelProvider(new PointLabelProvider());
		fPointListViewer.addSelectionChangedListener(this);
		fPointListViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event) {
				if (canFinish()){
					finish();
					wizard.getShell().close();
					wizard.dispose();
					wizard.setContainer(null);
				}
			}
		});

		fPointListViewer.setSorter(ListUtil.NAME_SORTER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		fPointListViewer.getTable().setLayoutData(gd);

		Composite templateComposite =
			new Composite(outerContainer, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 9;
		layout.marginWidth = 0;
		templateComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		templateComposite.setLayoutData(gd);
		
		templateLabel = new Label(templateComposite, SWT.NONE);
		templateLabel.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.contributedTemplates.title")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		templateLabel.setLayoutData(gd);
		
		SashForm templateSashForm = new SashForm(templateComposite, SWT.HORIZONTAL);
		templateSashForm.setLayout(new GridLayout());
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 80;
		templateSashForm.setLayoutData(gd);
		
		Composite wizardComposite =
			new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		wizardComposite.setLayout(layout);
		gd =
			new GridData(
				GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		wizardComposite.setLayoutData(gd);
		fTemplateViewer = new TableViewer(wizardComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fTemplateViewer.setContentProvider(new TemplateContentProvider());
		fTemplateViewer.setLabelProvider(ElementLabelProvider.INSTANCE);
		fTemplateViewer.setSorter(ListUtil.NAME_SORTER);
		fTemplateViewer.addSelectionChangedListener(this);
		gd = new GridData(GridData.FILL_BOTH);

		fTemplateViewer.getTable().setLayoutData(gd);  
		TableItem[] selection = fPointListViewer.getTable().getSelection();
		if (selection != null && selection.length > 0)
			fTemplateViewer.setInput((IPluginExtensionPoint)selection[0]);
		fTemplateViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event) {
				if (canFlipToNextPage()){
					advanceToNextPage();
				}
			}
		});
		
		Composite descriptionComposite =
			new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		descriptionComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		descriptionComposite.setLayoutData(gd);
		createDescriptionIn(descriptionComposite);

		
		fFilterCheck = new Button(outerContainer, SWT.CHECK);
		fFilterCheck.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.filterCheck")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFilterCheck.setLayoutData(gd);
		fFilterCheck.setSelection(true);
		fFilterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPointListViewer.refresh();
			}
		});
		
		getContainer().getShell().setSize(500, 500);
		createMenuManager();
		initialize();
		setControl(tabFolder);
		Dialog.applyDialogFont(outerContainer);
		WorkbenchHelp.setHelp(
			outerContainer,
			IHelpContextIds.ADD_EXTENSIONS_SCHEMA_BASED);
	}
	private Control createWizardsPage(Composite parent) {
		wizardsPage = new ExtensionTreeSelectionPage(wizardCollection, null, PDEPlugin.getResourceString("PointSelectionPage.categories")); //$NON-NLS-1$
		wizardsPage.createControl(parent);
		wizardsPage.setWizard(wizard);
		wizardsPage.getSelectionProvider().addSelectionChangedListener(this);
		wizardsPage.init(project, fPluginBase);
		return wizardsPage.getControl();
	}
	private void createMenuManager(){
		MenuManager mgr = new MenuManager();
		mgr.addMenuListener(new IMenuListener(){

			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
			
		});
		mgr.setRemoveAllWhenShown(true);
		Control control = fPointListViewer.getControl();
		Menu menu = mgr.createContextMenu(control);
		control.setMenu(menu);
	}
	private void fillContextMenu(IMenuManager mgr){
		mgr.add(showDetailsAction);
		ISelection selection = fPointListViewer.getSelection();
		IPluginExtensionPoint point = (IPluginExtensionPoint)((IStructuredSelection)selection).getFirstElement();
		showDetailsAction.setEnabled(point != null);
		
	}
	
	public void advanceToNextPage() {
		getContainer().showPage(getNextPage());
	}
	
	public boolean canFlipToNextPage() {
		return getNextPage() != null;
	}

	public boolean canFinish() {
		if (fTemplateViewer != null) {
			ISelection selection = fTemplateViewer.getSelection();
			if (selection instanceof IStructuredSelection){
				IStructuredSelection ssel = (IStructuredSelection)selection;
				if (!ssel.isEmpty())
					return false;
			}
		}
		if (fPointListViewer != null) {
			ISelection selection = fPointListViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.isEmpty() == false)
					return true;
			}
		}
		return false;
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		wizardsPage.dispose();
		super.dispose();
	}

	public boolean finish() {
		String point = fCurrentPoint.getFullId();
		
		//wizard.getEditor().ensurePluginContextPresence();
		try {
			IPluginExtension extension =
				fPluginBase.getModel().getFactory().createExtension();
			extension.setPoint(point);
			fPluginBase.add(extension);
			
			String pluginID = fCurrentPoint.getPluginBase().getId();
			if (!fAvailableImports.contains(pluginID)) {
				IPluginModelBase modelBase = ((IPluginModelBase) fPluginBase.getModel());
				IPluginImport importNode = modelBase.getPluginFactory().createImport();
				importNode.setId(pluginID);
				fPluginBase.add(importNode);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	private void doShowDescription() {
		if (fShowDescriptionAction == null)
			fShowDescriptionAction = new ShowDescriptionAction(fCurrentPoint);
		else
			fShowDescriptionAction.setExtensionPoint(fCurrentPoint);
		BusyIndicator.showWhile(fPointListViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				fShowDescriptionAction.run();
			}
		});
	}
	
	public IPluginExtension getNewExtension() {
		return fNewExtension;
	}
		
	protected void initialize() {
		fPointListViewer.addFilter(new PointFilter());
		fPointListViewer.setInput(PDECore.getDefault().getModelManager());
		fPointListViewer.getTable().setFocus();
	}
	
	private void makeActions(){
		showDetailsAction = new Action(){
			public void run(){
				doShowDescription();
			}
		};
		showDetailsAction.setText(PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.showDetails")); //$NON-NLS-1$
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		
		ISelection selection = event.getSelection();
		setDescription(""); //$NON-NLS-1$
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel != null && !ssel.isEmpty()) {
				if (ssel.getFirstElement() instanceof IPluginExtensionPoint){
					fCurrentPoint = (IPluginExtensionPoint) ssel.getFirstElement();
					fTemplateViewer.setInput(fCurrentPoint);
					if (fAvailableImports.contains(fCurrentPoint.getPluginBase().getId()))
						setMessage(null);
					else
						setMessage(
							PDEPlugin.getResourceString("NewExtensionWizard.PointSelectionPage.message"), //$NON-NLS-1$
							INFORMATION);
					setDescription(PDEPlugin.getFormattedMessage("NewExtensionWizard.PointSelectionPage.pluginDescription",fCurrentPoint.getFullId())); //$NON-NLS-1$
					setDescriptionText(""); //$NON-NLS-1$
					templateLabel.setText(PDEPlugin.getFormattedMessage("NewExtensionWizard.PointSelectionPage.contributedTemplates.label", fCurrentPoint.getFullId())); //$NON-NLS-1$
					setSelectedNode(null);
					setPageComplete(true);
				} else if (ssel.getFirstElement() instanceof WizardElement) {
					WizardElement wizardSelection = (WizardElement)ssel.getFirstElement();
					setSelectedNode(createWizardNode(wizardSelection));
					setDescriptionText(wizardSelection.getDescription());
					setDescription(PDEPlugin.getFormattedMessage("NewExtensionWizard.PointSelectionPage.templateDescription",wizardSelection.getLabel())); //$NON-NLS-1$
					setPageComplete(false);
				}
			}
			else {
				setSelectedNode(null);
				setPageComplete(false);
			}
		}
		getContainer().updateButtons();
	}
	
	private void updateTabSelection(int index) {
		if (index==0) {
			// extension point page
			ISelection selection = fTemplateViewer.getSelection();
			if (selection.isEmpty()==false)
				selectionChanged(new SelectionChangedEvent(fTemplateViewer, selection));
			else
				selectionChanged(new SelectionChangedEvent(fPointListViewer, fPointListViewer.getSelection()));
		}
		else {
			// wizard page
			ISelectionProvider provider = wizardsPage.getSelectionProvider();
			selectionChanged(new SelectionChangedEvent(provider, provider.getSelection()));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.BaseWizardSelectionPage#createWizardNode(org.eclipse.pde.internal.ui.wizards.WizardElement)
	 */
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				wizard.init(project, fPluginBase.getPluginModel());
				return wizard;
			}
			protected IExtensionWizard createWizard(WizardElement element)
			throws CoreException {
				if (element.isTemplate()) {
					IConfigurationElement template = element.getTemplateElement();
					if (template==null) return null;
					ITemplateSection section =
						(ITemplateSection) template.createExecutableExtension("class"); //$NON-NLS-1$
					return new NewExtensionTemplateWizard(section);
				} 
				return (IExtensionWizard) element.createExecutableExtension();
			}
		};
	}
}
