package org.eclipse.pde.internal.ui.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.layout.*;
import java.util.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.core.*;

public class PointSelectionPage
	extends WizardPage
	implements ISelectionChangedListener {
	private TableViewer pointListViewer;
	private TableViewer pluginListViewer;
	private IPluginBase pluginBase;
	private Text pointIdText;
	private Text pointNameText;
	private Label description;
	private Button descriptionButton;
	private Button filterCheck;
	private IPluginExtensionPoint currentPoint;
	private final static int SIZING_LISTS_HEIGHT = 200;
	private final static int SIZING_LISTS_WIDTH = 250;
	private final static String KEY_TITLE =
		"NewExtensionWizard.PointSelectionPage.title";
	private final static String KEY_POINT_ID =
		"NewExtensionWizard.PointSelectionPage.pointId";
	private final static String KEY_POINT_NAME =
		"NewExtensionWizard.PointSelectionPage.pointName";
	private final static String KEY_DESC =
		"NewExtensionWizard.PointSelectionPage.desc";
	private final static String KEY_MISSING_TITLE =
		"NewExtensionWizard.PointSelectionPage.missingTitle";
	private final static String KEY_MISSING_IMPORT =
		"NewExtensionWizard.PointSelectionPage.missingImport";
		
	private final static String KEY_FILTER_CHECK =
			"NewExtensionWizard.PointSelectionPage.filterCheck";
	private final static String KEY_DESC_BUTTON =
			"NewExtensionWizard.PointSelectionPage.descButton";
	private final static String KEY_WARNING =
			"NewExtensionWizard.PointSelectionPage.warning";
	private IPluginExtension newExtension;
	private ShowDescriptionAction showDescriptionAction;
	
	class PointFilter extends ViewerFilter {
		public boolean select(
			Viewer viewer,
			Object parentElement,
			Object element) {
			if (!filterCheck.getSelection()) return true;
			ExPoint ep = (ExPoint)element;
			return ep.isFromDependency();
		}
	}

	class ExPoint {
		boolean fromDependency;
		IPluginExtensionPoint point;
		public ExPoint(IPluginExtensionPoint point, boolean fromDependency) {
			this.point = point;
			this.fromDependency = fromDependency;
		}
		public IPluginExtensionPoint getPoint() {
			return point;
		}
		public boolean isFromDependency() {
			return fromDependency;
		}
	}

	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		private Vector points = null;
		public Object[] getElements(Object parent) {
			if (parent instanceof ExternalModelManager) {
				if (points == null) {
					points = new Vector();
					WorkspaceModelManager manager =
						PDECore.getDefault().getWorkspaceModelManager();
					addPoints(manager.getWorkspacePluginModels());
					ExternalModelManager registry =
						(ExternalModelManager) parent;
					addPoints(registry.getModels());
				}
				Object[] result = new Object[points.size()];
				points.copyInto(result);
				return result;
			}
			return new Object[0];
		}
		private void addPoints(IPluginModel[] models) {
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				if (model.isEnabled()) {
					IPlugin pluginInfo = model.getPlugin();
					PointSelectionPage.this.addPoints(pluginInfo, points);
				}
			}
		}
	}

	class PointLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public String getColumnText(Object obj, int index) {
			if (obj instanceof ExPoint) {
				PDELabelProvider provider =
					PDEPlugin.getDefault().getLabelProvider();
				IPluginExtensionPoint point = ((ExPoint) obj).getPoint();
				if (provider.isFullNameModeEnabled())
					return provider.getText(point);
				return (point).getFullId();
			}
			return obj.toString();
		}
		public Image getImage(Object obj) {
			return getColumnImage(obj, 0);
		}
		public Image getColumnImage(Object obj, int index) {
			ExPoint exp = (ExPoint) obj;
			int flag =
				exp.isFromDependency() ? 0 : SharedLabelProvider.F_WARNING;
			return PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_EXT_POINT_OBJ,
				flag);
		}
	}

	public PointSelectionPage(IPluginBase model) {
		super("pointSelectionPage");
		this.pluginBase = model;
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		provider.connect(this);
	}

	void addPoints(IPluginBase pluginBase, Vector points) {
		IPluginExtensionPoint[] pts = pluginBase.getExtensionPoints();
		for (int i = 0; i < pts.length; i++) {
			IPluginExtensionPoint pt = pts[i];
			boolean fromDependency = isFromDependency(pt);
			points.addElement(new ExPoint(pt, fromDependency));
		}
		if (pluginBase instanceof IPlugin
			&& pluginBase.getModel().getUnderlyingResource() != null) {
			// merge points from fragments
			WorkspaceModelManager manager =
				PDECore.getDefault().getWorkspaceModelManager();
			IFragment[] fragments =
				manager.getFragmentsFor(
					pluginBase.getId(),
					pluginBase.getVersion());
			for (int i = 0; i < fragments.length; i++) {
				addPoints(fragments[i], points);
			}
		}
	}
	public void createControl(Composite parent) {
		// top level group
		Composite outerContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
			new GridData(
				GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		filterCheck = new Button(outerContainer, SWT.CHECK);
		filterCheck.setText(PDEPlugin.getResourceString(KEY_FILTER_CHECK));
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		filterCheck.setLayoutData(gd);
		filterCheck.setSelection(true);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pointListViewer.refresh();
			}
		});

		pointListViewer =
			new TableViewer(
				outerContainer,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		pointListViewer.setContentProvider(new ContentProvider());
		pointListViewer.setLabelProvider(new PointLabelProvider());
		pointListViewer.addSelectionChangedListener(this);
		pointListViewer.setSorter(ListUtil.NAME_SORTER);

		gd =
			new GridData(
				GridData.FILL_BOTH
					| GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.horizontalSpan = 2;
		pointListViewer.getTable().setLayoutData(gd);

		descriptionButton = new Button(outerContainer, SWT.PUSH);
		descriptionButton.setText(PDEPlugin.getResourceString(KEY_DESC_BUTTON));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		descriptionButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(descriptionButton);
		descriptionButton.setEnabled(false);
		descriptionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doShowDescription();
			}
		});

		Label label = new Label(outerContainer, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_POINT_ID));
		pointIdText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		pointIdText.setLayoutData(gd);
		new Label(outerContainer, SWT.NULL);

		label = new Label(outerContainer, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_POINT_NAME));
		pointNameText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		pointNameText.setLayoutData(gd);
		new Label(outerContainer, SWT.NULL);

		createDescriptionIn(outerContainer);
		initialize();
		setControl(outerContainer);
		Dialog.applyDialogFont(outerContainer);
		WorkbenchHelp.setHelp(
			outerContainer,
			IHelpContextIds.ADD_EXTENSIONS_SCHEMA_BASED);
	}

	public boolean canFinish() {
		if (pointListViewer != null) {
			ISelection selection = pointListViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.isEmpty() == false)
					return true;
			}
		}
		return false;
	}

	public void createDescriptionIn(Composite composite) {
		description = new Label(composite, SWT.NONE);
		GridData gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		description.setLayoutData(gd);
	}
	public void dispose() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		provider.disconnect(this);
		super.dispose();
	}
	public boolean finish() {
		String id = pointIdText.getText();
		if (id.length() == 0)
			id = null;

		String name = pointNameText.getText();
		if (name.length() == 0)
			name = null;

		String point = currentPoint.getFullId();

		try {
			if (!ensureImportExists(currentPoint))
				return false;
			IPluginExtension extension =
				pluginBase.getModel().getFactory().createExtension();
			extension.setName(name);
			extension.setPoint(point);
			if (id != null)
				extension.setId(id);
			pluginBase.add(extension);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	private void doShowDescription() {
		if (showDescriptionAction == null)
			showDescriptionAction = new ShowDescriptionAction(currentPoint);
		else
			showDescriptionAction.setExtensionPoint(currentPoint);
		BusyIndicator
			.showWhile(descriptionButton.getDisplay(), new Runnable() {
			public void run() {
				showDescriptionAction.run();
			}
		});
	}

	private boolean isFromDependency(IPluginExtensionPoint point) {
		IPlugin thisPlugin = getTargetPlugin(pluginBase);
		IPlugin exPlugin = getTargetPlugin(point.getPluginBase());
		if (thisPlugin == null || exPlugin == null)
			return true;

		String exId = exPlugin.getId();
		// Check if it is us
		if (exId.equals(thisPlugin.getId()))
			return true;
		//Check if it is implicit
		if (exId.equals("org.eclipse.core.boot")
			|| exId.equals("org.eclipse.core.runtime"))
			return true;
		// We must have it

		IPluginImport[] iimports = thisPlugin.getImports();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			if (iimport.getId().equals(exId)) {
				// found it
				return true;
			}
		}
		return false;
	}

	private boolean ensureImportExists(IPluginExtensionPoint point)
		throws CoreException {
		if (isFromDependency(point))
			return true;
		// Don't have it - warn
		IPlugin exPlugin = getTargetPlugin(point.getPluginBase());
		if (exPlugin == null)
			return true;
		String[] args =
			{
				point.getResourceString(point.getName()),
				exPlugin.getResourceString(exPlugin.getName())};
		String message =
			PDEPlugin.getFormattedMessage(KEY_MISSING_IMPORT, args);
		MessageDialog.openWarning(
			PDEPlugin.getActiveWorkbenchShell(),
			PDEPlugin.getResourceString(KEY_MISSING_TITLE),
			message);
		return false;
	}

	private IPlugin getTargetPlugin(IPluginBase base) {
		if (base instanceof IPlugin)
			return (IPlugin) base;
		else {
			IFragment fragment = (IFragment) base;
			String targetId = fragment.getPluginId();
			String targetVersion = fragment.getPluginVersion();
			int match = fragment.getRule();
			return PDECore.getDefault().findPlugin(
				targetId,
				targetVersion,
				match);
		}
	}

	public IPluginExtension getNewExtension() {
		return newExtension;
	}
	protected void initialize() {
		pointListViewer.addFilter(new PointFilter());
		pointListViewer.setInput(
			PDECore.getDefault().getExternalModelManager());
		pointListViewer.getTable().setFocus();
	}
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();

		ExPoint input = null;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Iterator elements = ssel.iterator();
			if (elements.hasNext()) {
				input = (ExPoint) elements.next();
				if (elements.hasNext())
					input = null;
			}
		}
		boolean fromDependency = input != null && input.isFromDependency();
		setPageComplete(input != null && fromDependency);
		descriptionButton.setEnabled(input != null);
		String message = null;
		if (!fromDependency)
			message = PDEPlugin.getResourceString(KEY_WARNING);
		setMessage(message, WARNING);
		currentPoint = input!=null? input.getPoint():null;
		String description = "";
		if (currentPoint != null)
			description = currentPoint.getFullId();
		setDescriptionText(description);
		getContainer().updateButtons();
	}
	public void setDescriptionText(String text) {
		description.setText(text);
	}
}
