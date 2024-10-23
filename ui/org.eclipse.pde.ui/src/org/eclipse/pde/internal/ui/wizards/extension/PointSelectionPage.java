/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaAnnotationHandler;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionNode;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.PDEHTMLHelper;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.core.util.SchemaUtil;
import org.eclipse.pde.internal.core.util.XMLComponentRegistry;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLInsertionComputer;
import org.eclipse.pde.internal.ui.elements.ElementLabelProvider;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.pde.internal.ui.wizards.BaseWizardSelectionPage;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.WizardCollectionElement;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.internal.ui.wizards.WizardNode;
import org.eclipse.pde.internal.ui.wizards.templates.NewExtensionTemplateWizard;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IExtensionWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class PointSelectionPage extends BaseWizardSelectionPage {
	private TableViewer fPointListViewer;
	private TableViewer fTemplateViewer;

	private final IPluginModelBase fModel;
	private Button fFilterCheck;
	private IPluginExtensionPoint fCurrentPoint;
	private final Map<String, Boolean> fAvailableImports;
	private final IProject fProject;
	private Label fTemplateLabel;
	private ExtensionTreeSelectionPage fWizardsPage;

	private IPluginExtension fNewExtension;
	private final WizardCollectionElement fTemplateCollection;
	private final WizardCollectionElement fWizardCollection;
	private final NewExtensionWizard fWizard;
	private Text fFilterText;
	private final WildcardFilter fWildCardFilter;
	private Text fPointDescription;
	private Link fDescLink;
	private Browser fPointDescBrowser;

	class PointFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!fFilterCheck.getSelection())
				return true;

			IPluginExtensionPoint point = (IPluginExtensionPoint) element;
			if (point instanceof PluginExtensionPointNode)
				return true;

			return fAvailableImports.keySet().contains(point.getPluginBase().getId());
		}
	}

	class WildcardFilter extends ViewerFilter {
		private String wMatch = "*"; //$NON-NLS-1$

		protected void setMatchText(String match) {
			if (match.indexOf('*') != 0 && match.indexOf('?') != 0 && match.indexOf('.') != 0) {
				match = "*" + match; //$NON-NLS-1$
			}
			wMatch = match + "*"; //$NON-NLS-1$
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String text = ((PointLabelProvider) fPointListViewer.getLabelProvider()).getColumnText(element, 0);
			Pattern pattern = null;
			try {
				pattern = PatternConstructor.createPattern(wMatch, false);
			} catch (PatternSyntaxException e) {
				return false;
			}
			return pattern != null && pattern.matcher(text.subSequence(0, text.length())).matches();
		}
	}

	class TemplateContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IPluginExtensionPoint point) {
				String pointID = IdUtil.getFullId(point, fModel);
				ArrayList<Object> result = new ArrayList<>();
				if (fTemplateCollection.getWizards() != null) {
					Object[] wizards = fTemplateCollection.getWizards().getChildren();
					for (Object wizardObject : wizards) {
						String wizardContributorId = ((WizardElement) wizardObject).getContributingId();
						if (wizardContributorId == null || pointID == null)
							continue;
						if (wizardObject instanceof WizardElement && wizardContributorId.equals(pointID))
							result.add(wizardObject);
					}
					return result.toArray();
				}
			}
			return new Object[0];
		}
	}

	class PointContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			ArrayList<IPluginExtensionPoint> extPoints = new ArrayList<>();
			IPluginModelBase[] plugins = PluginRegistry.getActiveModels();
			for (IPluginModelBase plugin : plugins) {
				IPluginExtensionPoint[] points = plugin.getPluginBase().getExtensionPoints();
				String id = plugin.getPluginBase().getId();
				if (id.equals(fModel.getPluginBase().getId()))
					continue;
				Collections.addAll(extPoints, points);
			}

			IPluginExtensionPoint[] points = fModel.getPluginBase().getExtensionPoints();
			Collections.addAll(extPoints, points);

			return extPoints.toArray();
		}
	}

	class PointLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}

		@Override
		public String getColumnText(Object obj, int index) {
			IPluginExtensionPoint extPoint = (IPluginExtensionPoint) obj;
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			if (provider.isFullNameModeEnabled())
				return provider.getText(extPoint);

			return IdUtil.getFullId(extPoint, fModel);
		}

		@Override
		public Image getImage(Object obj) {
			return getColumnImage(obj, 0);
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			IPluginExtensionPoint exp = (IPluginExtensionPoint) obj;

			if (((TemplateContentProvider) fTemplateViewer.getContentProvider()).getElements(exp).length > 0) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_NEWEXP_WIZ_TOOL, 0);
			}

			// If the schema is deprecated add a warning flag
			int flags = 0;
			SchemaRegistry reg = PDECore.getDefault().getSchemaRegistry();
			ISchema schema = reg.getSchema(exp.getFullId());
			if (schema != null && schema.isDeperecated()) {
				PDEPlugin.getDefault().getLabelProvider();
				flags = SharedLabelProvider.F_WARNING;
			}

			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_EXT_POINT_OBJ, flags);
		}
	}

	public PointSelectionPage(IProject project, IPluginModelBase model, WizardCollectionElement element, WizardCollectionElement templates, NewExtensionWizard wizard) {
		super("pointSelectionPage", PDEUIMessages.NewExtensionWizard_PointSelectionPage_title); //$NON-NLS-1$
		this.fModel = model;
		this.fWizardCollection = element;
		this.fTemplateCollection = templates;
		this.fWizard = wizard;
		this.fProject = project;
		fWildCardFilter = new WildcardFilter();
		fAvailableImports = PluginSelectionDialog.getExistingImports(model, true);
		setTitle(PDEUIMessages.NewExtensionWizard_PointSelectionPage_title);
		setDescription(PDEUIMessages.NewExtensionWizard_PointSelectionPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		// tab folder
		final CTabFolder tabFolder = new CTabFolder(parent, SWT.FLAT);
		CTabItem firstTab = new CTabItem(tabFolder, SWT.NULL);
		firstTab.setText(PDEUIMessages.PointSelectionPage_tab1);
		CTabItem secondTab = new CTabItem(tabFolder, SWT.NULL);
		secondTab.setText(PDEUIMessages.PointSelectionPage_tab2);
		secondTab.setControl(createWizardsPage(tabFolder));
		tabFolder.addSelectionListener(widgetSelectedAdapter(e -> updateTabSelection(tabFolder.getSelectionIndex())));
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

		Composite labelContainer = new Composite(pointContainer, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		labelContainer.setLayout(layout);
		labelContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label filterLabel = new Label(labelContainer, SWT.NONE);
		filterLabel.setText(PDEUIMessages.NewExtensionWizard_PointSelectionPage_availExtPoints_label);
		gd = new GridData();
		gd.verticalAlignment = GridData.CENTER;
		filterLabel.setLayoutData(gd);
		fFilterText = new Text(labelContainer, SWT.BORDER);
		fFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFilterText.addModifyListener(e -> {
			fWildCardFilter.setMatchText(fFilterText.getText());
			refreshPointListViewer();
		});
		fFilterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN)
					fPointListViewer.getControl().setFocus();
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		fFilterCheck = new Button(outerContainer, SWT.CHECK);
		fFilterCheck.setText(PDEUIMessages.NewExtensionWizard_PointSelectionPage_filterCheck);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFilterCheck.setLayoutData(gd);
		fFilterCheck.setSelection(true);
		fFilterCheck.addSelectionListener(widgetSelectedAdapter(e -> refreshPointListViewer()));

		fPointListViewer = new TableViewer(pointContainer, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fPointListViewer.setContentProvider(new PointContentProvider());
		fPointListViewer.setLabelProvider(new PointLabelProvider());
		fPointListViewer.addSelectionChangedListener(this);
		fPointListViewer.addDoubleClickListener(event -> {
			if (canFinish()) {
				fWizard.performFinish();
				fWizard.getShell().close();
				fWizard.dispose();
				fWizard.setContainer(null);
			}
		});
		fPointListViewer.addFilter(fWildCardFilter);
		fPointListViewer.setComparator(ListUtil.NAME_COMPARATOR);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		fPointListViewer.getTable().setLayoutData(gd);

		Composite templateComposite = new Composite(outerContainer, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 4;
		layout.marginWidth = 0;
		templateComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		templateComposite.setLayoutData(gd);

		fDescLink = new Link(templateComposite, SWT.NONE);
		fDescLink.setText(NLS.bind(PDEUIMessages.PointSelectionPage_extPointDesc, "")); //$NON-NLS-1$
		fDescLink.addSelectionListener(widgetSelectedAdapter(e -> {
			if (fCurrentPoint != null)
				new ShowDescriptionAction(fCurrentPoint, true).run();
		}));
		fDescLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control c = null;
		Composite comp = new Composite(templateComposite, SWT.BORDER);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		try {
			c = fPointDescBrowser = new Browser(comp, SWT.NONE);
		} catch (SWTError e) {
		}
		if (c == null)
			c = fPointDescription = new Text(comp, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);

		setPointDescriptionText(PDEUIMessages.NewExtensionWizard_PointSelectionPage_extPointDescription);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 80;
		c.setLayoutData(gd);

		fTemplateLabel = new Label(templateComposite, SWT.NONE | SWT.WRAP);
		fTemplateLabel.setText(PDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_title);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fTemplateLabel.setLayoutData(gd);

		SashForm templateSashForm = new SashForm(templateComposite, SWT.HORIZONTAL);
		templateSashForm.setLayout(new GridLayout());
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 80;
		templateSashForm.setLayoutData(gd);

		Composite wizardComposite = new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		wizardComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		wizardComposite.setLayoutData(gd);
		fTemplateViewer = new TableViewer(wizardComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fTemplateViewer.setContentProvider(new TemplateContentProvider());
		fTemplateViewer.setLabelProvider(ElementLabelProvider.INSTANCE);
		fTemplateViewer.setComparator(ListUtil.NAME_COMPARATOR);
		fTemplateViewer.addSelectionChangedListener(this);
		gd = new GridData(GridData.FILL_BOTH);

		fTemplateViewer.getTable().setLayoutData(gd);
		TableItem[] selection = fPointListViewer.getTable().getSelection();
		if (selection != null && selection.length > 0)
			fTemplateViewer.setInput(selection[0]);
		fTemplateViewer.addDoubleClickListener(event -> {
			if (canFlipToNextPage()) {
				advanceToNextPage();
			}
		});

		Composite descriptionComposite = new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		descriptionComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		descriptionComposite.setLayoutData(gd);
		createDescriptionIn(descriptionComposite);

		initialize();
		setControl(tabFolder);
		Dialog.applyDialogFont(outerContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(outerContainer.getParent(), IHelpContextIds.ADD_EXTENSIONS_SCHEMA_BASED);
	}

	private void refreshPointListViewer() {
		Control control = fPointListViewer.getControl();
		try {
			control.setRedraw(false);
			fPointListViewer.refresh();
		} finally {
			control.setRedraw(true);
		}
	}

	private Control createWizardsPage(Composite parent) {
		fWizardsPage = new ExtensionTreeSelectionPage(fWizardCollection, null, PDEUIMessages.PointSelectionPage_categories);
		fWizardsPage.createControl(parent);
		fWizardsPage.setWizard(fWizard);
		fWizardsPage.getSelectionProvider().addSelectionChangedListener(this);
		fWizardsPage.init(fProject, fModel.getPluginBase());
		return fWizardsPage.getControl();
	}

	public void advanceToNextPage() {
		getContainer().showPage(getNextPage());
	}

	@Override
	public boolean canFlipToNextPage() {
		return getNextPage() != null;
	}

	public boolean canFinish() {
		if (fTemplateViewer != null) {
			IStructuredSelection selection = fTemplateViewer.getStructuredSelection();
			if (!selection.isEmpty())
				return false;
		}
		if (fPointListViewer != null) {
			IStructuredSelection selection = fPointListViewer.getStructuredSelection();
			if (selection.isEmpty() == false)
				return true;
		}
		return false;
	}

	@Override
	public void dispose() {
		fWizardsPage.dispose();
		super.dispose();
	}

	public boolean finish() {
		String point = IdUtil.getFullId(fCurrentPoint, fModel);

		try {
			IPluginExtension extension = fModel.getFactory().createExtension();
			// Set the point attribute
			// The point value overrides an auto-generated value
			extension.setPoint(point);
			fModel.getPluginBase().add(extension);

			// Recursively auto-insert required child elements and attributes
			// respecting multiplicity
			ISchemaElement schemaElement = null;
			// Get the extension's schema
			Object object = extension.getSchema();
			if ((object != null) && (object instanceof Schema schema)) {
				if (extension instanceof PluginExtensionNode) {
					// Get the extension's XML element name
					String elementName = ((PluginExtensionNode) extension).getXMLTagName();
					// Find the extension's corresponding schema element
					schemaElement = schema.findElement(elementName);
				}
				// If there is an associated schema, do the auto-insert
				if (schemaElement != null) {
					XMLInsertionComputer.computeInsertion(schemaElement, extension);
				}
			}

			String pluginID = fCurrentPoint.getPluginBase().getId();
			if (!(fCurrentPoint instanceof PluginExtensionPointNode) && !fAvailableImports.keySet().contains(pluginID) && !(fCurrentPoint.getPluginBase() instanceof IFragment)) {
				if (MessageDialog.openQuestion(getShell(), PDEUIMessages.NewExtensionWizard_PointSelectionPage_dependencyTitle, NLS.bind(PDEUIMessages.NewExtensionWizard_PointSelectionPage_dependencyMessage, new String[] {pluginID, fCurrentPoint.getId()}))) {
					IPluginImport importNode = fModel.getPluginFactory().createImport();
					importNode.setId(pluginID);
					fModel.getPluginBase().add(importNode);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	public IPluginExtension getNewExtension() {
		return fNewExtension;
	}

	protected void initialize() {
		fPointListViewer.addFilter(new PointFilter());
		fPointListViewer.setInput(PDECore.getDefault().getModelManager());
		fPointListViewer.getTable().setFocus();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection ssel) {
			if (!ssel.isEmpty()) {
				Object element = ssel.getFirstElement();
				if (element instanceof WizardElement)
					handleTemplateSelection((WizardElement) element);
				else if (element instanceof IPluginExtensionPoint)
					handlePointSelection((IPluginExtensionPoint) element);
			} else {
				setDescription(""); //$NON-NLS-1$
				setDescriptionText(""); //$NON-NLS-1$
				fTemplateLabel.setText(PDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_title);
				setPointDescriptionText(PDEUIMessages.PointSelectionPage_noDescAvailable);
				fDescLink.setText(NLS.bind(PDEUIMessages.PointSelectionPage_extPointDesc, "")); //$NON-NLS-1$
				setSelectedNode(null);
				setPageComplete(false);
			}
			getContainer().updateButtons();
		}
	}

	private void handleTemplateSelection(WizardElement element) {
		setSelectedNode(createWizardNode(element));
		setDescriptionText(element.getDescription());
		setDescription(NLS.bind(PDEUIMessages.NewExtensionWizard_PointSelectionPage_templateDescription, element.getLabel()));
		setPageComplete(false);
	}

	private void handlePointSelection(IPluginExtensionPoint element) {
		fCurrentPoint = element;
		fTemplateViewer.setInput(fCurrentPoint);
		fTemplateViewer.setSelection(StructuredSelection.EMPTY);
		String fullPointID = IdUtil.getFullId(fCurrentPoint, fModel);

		String description = XMLComponentRegistry.Instance().getDescription(fullPointID, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		String name = XMLComponentRegistry.Instance().getName(fullPointID, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		URL url = null;
		if ((description == null) || (name == null)) {
			url = SchemaRegistry.getSchemaURL(fCurrentPoint, fModel);
		}
		if (url != null) {
			SchemaAnnotationHandler handler = new SchemaAnnotationHandler();
			SchemaUtil.parseURL(url, handler);
			description = handler.getDescription();
			name = handler.getName();
		}
		if (description == null) {
			setPointDescriptionText(PDEUIMessages.PointSelectionPage_noDescAvailable);
		} else {
			setPointDescriptionText(description);
		}
		if (name == null) {
			name = fullPointID;
		}
		// Check if the extension point is deprecated and display a warning
		SchemaRegistry reg = PDECore.getDefault().getSchemaRegistry();
		ISchema schema = reg.getSchema(fCurrentPoint.getFullId());
		if (schema != null && schema.isDeperecated()) {
			setMessage(NLS.bind(PDEUIMessages.NewExtensionWizard_PointSelectionPage_pluginDescription_deprecated, name), IMessageProvider.WARNING);
		} else {
			setMessage(null);
			setDescription(NLS.bind(PDEUIMessages.NewExtensionWizard_PointSelectionPage_pluginDescription, name));
		}
		setDescriptionText(""); //$NON-NLS-1$
		fTemplateLabel.setText(NLS.bind(PDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_label, name.toLowerCase(Locale.ENGLISH)));
		fDescLink.setText(NLS.bind(PDEUIMessages.PointSelectionPage_extPointDesc, name));
		setSelectedNode(null);
		setPageComplete(true);

		XMLComponentRegistry.Instance().putDescription(fullPointID, description, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		XMLComponentRegistry.Instance().putName(fullPointID, name, XMLComponentRegistry.F_SCHEMA_COMPONENT);
	}

	private void updateTabSelection(int index) {
		if (index == 0) {
			// extension point page
			IStructuredSelection selection = fTemplateViewer.getStructuredSelection();
			if (selection.isEmpty() == false)
				selectionChanged(new SelectionChangedEvent(fTemplateViewer, selection));
			else
				selectionChanged(
						new SelectionChangedEvent(fPointListViewer, fPointListViewer.getStructuredSelection()));
			fFilterText.setFocus();
		} else {
			// wizard page
			ISelectionProvider provider = fWizardsPage.getSelectionProvider();
			selectionChanged(new SelectionChangedEvent(provider, provider.getSelection()));
		}
	}

	@Override
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			@Override
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				if (wizard == null)
					throw new CoreException(Status.error(PDEUIMessages.PointSelectionPage_cannotFindTemplate));
				wizard.init(fProject, fModel);
				return wizard;
			}

			protected IExtensionWizard createWizard(WizardElement element) throws CoreException {
				if (element.isTemplate()) {
					IConfigurationElement template = element.getTemplateElement();
					if (template == null)
						return null;
					ITemplateSection section = (ITemplateSection) template.createExecutableExtension("class"); //$NON-NLS-1$
					return new NewExtensionTemplateWizard(section);
				}
				return (IExtensionWizard) element.createExecutableExtension();
			}
		};
	}

	public void checkModel() {
		IWizardNode node = getSelectedNode();
		if (node == null)
			return;
		IWizard wizard = node.getWizard();
		if (wizard instanceof NewExtensionTemplateWizard) {
			if (((NewExtensionTemplateWizard) wizard).updatedDependencies()) {
				if (MessageDialog.openQuestion(getShell(), PDEUIMessages.PointSelectionPage_newDepFound, PDEUIMessages.PointSelectionPage_newDepMessage)) {
					fWizard.getEditor().doSave(new NullProgressMonitor());
				}
			}
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			fFilterText.setFocus();
		super.setVisible(visible);
	}

	private void setPointDescriptionText(String text) {
		if (fPointDescBrowser != null) {
			StringBuilder desc = new StringBuilder();

			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			Color fgcolor = colorRegistry.get(JFacePreferences.INFORMATION_FOREGROUND_COLOR);
			if (fgcolor == null) {
				fgcolor = JFaceColors.getInformationViewerForegroundColor(Display.getDefault());
			}
			Color bgcolor = colorRegistry.get(JFacePreferences.INFORMATION_BACKGROUND_COLOR);
			if (bgcolor == null) {
				bgcolor = JFaceColors.getInformationViewerForegroundColor(Display.getDefault());
			}

			StringBuilder stylesheet = new StringBuilder();
			String line;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(TextUtil.getJavaDocStyleSheerURL().openStream()))) {
				while ((line = br.readLine()) != null) {
					stylesheet.append('\n');
					stylesheet.append(line);
				}
			} catch (IOException e) {
				// continue, and attempt to render without stylesheet
				stylesheet.setLength(0);
			}

			HTMLPrinter.insertPageProlog(desc, 0, fgcolor.getRGB(), bgcolor.getRGB(),
					stylesheet.length() > 0 ? stylesheet.substring(1) : ""); //$NON-NLS-1$
			desc.append(text);
			HTMLPrinter.addPageEpilog(desc);
			fPointDescBrowser.setText(desc.toString());
		} else
			fPointDescription.setText(PDEHTMLHelper.stripTags(text));
	}
}
