package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.layout.*;
import java.util.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.MessageDialog;


public class PointSelectionPage
	extends WizardPage
	implements ISelectionChangedListener {
	private TableViewer pointListViewer;
	private TableViewer pluginListViewer;
	private IPluginBase pluginBase;
	private Text pointIdText;
	private Text pointNameText;
	private Label description;
	private IPluginExtensionPoint currentPoint;
	private final static int SIZING_LISTS_HEIGHT = 200;
	private final static int SIZING_LISTS_WIDTH = 250;
	private final static String KEY_TITLE = "NewExtensionWizard.PointSelectionPage.title";
	private final static String KEY_POINT_ID = "NewExtensionWizard.PointSelectionPage.pointId";
	private final static String KEY_POINT_NAME = "NewExtensionWizard.PointSelectionPage.pointName";
	private final static String KEY_DESC = "NewExtensionWizard.PointSelectionPage.desc";
	private final static String KEY_MISSING_TITLE = "NewExtensionWizard.PointSelectionPage.missingTitle";
	private final static String KEY_MISSING_IMPORT = "NewExtensionWizard.PointSelectionPage.missingImport";
	private Image pointImage;
	private IPluginExtension newExtension;

	public class NameSorter extends ViewerSorter {
		public boolean isSorterProperty(Object element, Object propertyId) {
			return true;
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
					WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
					addPoints(manager.getWorkspacePluginModels());
					ExternalModelManager registry = (ExternalModelManager) parent;
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

	class PointLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			IPluginExtensionPoint info = (IPluginExtensionPoint) obj;
			if (index == 0)
				return info.getTranslatedName();
			return "";
		}
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return pointImage;
			return null;
		}
	}

public PointSelectionPage(IPluginBase model) {
	super("pointSelectionPage");
	this.pluginBase = model;
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	pointImage = PDEPluginImages.DESC_EXT_POINT_OBJ.createImage();
}
void addPoints(IPluginBase pluginBase, Vector points) {
	IPluginExtensionPoint[] pts = pluginBase.getExtensionPoints();
	for (int i = 0; i < pts.length; i++) {
		points.addElement(pts[i]);
	}
	if (pluginBase instanceof IPlugin
		&& pluginBase.getModel().getUnderlyingResource() != null) {
		// merge points from fragments
		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		IFragment[] fragments =
			manager.getFragmentsFor(pluginBase.getId(), pluginBase.getVersion());
		for (int i = 0; i < fragments.length; i++) {
			addPoints(fragments[i], points);
		}
	}
}
public void createControl(Composite parent) {
	// top level group
	Composite outerContainer = new Composite(parent,SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	outerContainer.setLayout(layout);
	outerContainer.setLayoutData(new GridData(
		GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

	Table table = new Table(outerContainer, SWT.BORDER);
	new TableColumn(table, SWT.NONE);
	TableLayout tlayout = new TableLayout();
	tlayout.addColumnData(new ColumnWeightData(100));
	table.setLayout(tlayout);
		
	// plugin pane
	pointListViewer = new TableViewer(table);
	pointListViewer.setContentProvider(new ContentProvider());
	pointListViewer.setLabelProvider(new PointLabelProvider());
	pointListViewer.addSelectionChangedListener(this);

	GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	gd.heightHint = 300;
	gd.horizontalSpan = 2;
	table.setLayoutData(gd);

	Label label = new Label(outerContainer, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(KEY_POINT_ID));
	pointIdText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
	pointIdText.setLayoutData(gd);

	label = new Label(outerContainer, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(KEY_POINT_NAME));
	pointNameText = new Text(outerContainer, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
	pointNameText.setLayoutData(gd);

	createDescriptionIn(outerContainer);
	initialize();
	setControl(outerContainer);
}

public boolean canFinish() {
	if (pointListViewer!=null) {
		ISelection selection = pointListViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			if (ssel.isEmpty()==false) return true;
		}
	}
	return false;
}

public void createDescriptionIn(Composite composite) {
	description = new Label(composite, SWT.NONE);
	GridData gd =
		new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
	gd.horizontalSpan = 2;
	description.setLayoutData(gd);
}
public void dispose() {
	pointImage.dispose();
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
		IPluginExtension extension = pluginBase.getModel().getFactory().createExtension();
		extension.setName(name);
		extension.setPoint(point);
		if (id != null)
			extension.setId(id);
		pluginBase.add(extension);
		ensureImportExists(pluginBase, currentPoint);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
	return true;
}

private void ensureImportExists(IPluginBase pluginBase, IPluginExtensionPoint point) throws CoreException {
	IPlugin thisPlugin = getTargetPlugin(pluginBase);
	IPlugin exPlugin = getTargetPlugin(point.getPluginBase());
	if (thisPlugin==null || exPlugin==null) return;
	
	String exId = exPlugin.getId();
	// Check if it is us
	if (exId.equals(thisPlugin.getId())) return;
	//Check if it is implicit
	if (exId.equals("org.eclipse.core.boot") ||
		exId.equals("org.eclipse.core.runtime")) return;
	// We must have it
	
	IPluginImport [] iimports = thisPlugin.getImports();
	for (int i=0; i<iimports.length; i++) {
		IPluginImport iimport = iimports[i];
		if (iimport.getId().equals(exId)) {
			// found it
			return;
		}
	}
	// Don't have it - warn
	String [] args = { point.getResourceString(point.getName()), 
					   exPlugin.getResourceString(exPlugin.getName()) };
	String message = PDEPlugin.getFormattedMessage(KEY_MISSING_IMPORT, 
													args);
	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(KEY_MISSING_TITLE),
					message);
}

private IPlugin getTargetPlugin(IPluginBase base) {
	if (base instanceof IPlugin) return (IPlugin)base;
	else {
		IFragment fragment = (IFragment)base;
		String targetId = fragment.getPluginId();
		String targetVersion = fragment.getPluginVersion();
		int match = fragment.getRule();
		return PDEPlugin.getDefault().findPlugin(targetId, targetVersion, match);
	}
}


public IPluginExtension getNewExtension() {
	return newExtension;
}
protected void initialize() {
	pointListViewer.setSorter(new NameSorter());
	pointListViewer.setInput(PDEPlugin.getDefault().getExternalModelManager());
	pointListViewer.getTable().setFocus();
}
public void selectionChanged(SelectionChangedEvent event) {
	ISelection selection = event.getSelection();
	ISelectionProvider provider = event.getSelectionProvider();

	Object input=null;

	if (selection instanceof IStructuredSelection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		Iterator elements = ssel.iterator();
		if (elements.hasNext()) {
			input = elements.next();
			if (elements.hasNext()) input = null;
		}
	}
	setPageComplete(input!=null);
	currentPoint = (IPluginExtensionPoint)input;
	String description = "";
	if (currentPoint!=null) description = currentPoint.getFullId();
	setDescriptionText(description);
	getContainer().updateButtons();
}
public void setDescriptionText(String text) {
	description.setText(text);
}
}
