package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.tasklist.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.core.builders.DependencyLoop;
import org.eclipse.pde.internal.core.builders.DependencyLoopFinder;


public class AlertSection
	extends PDEFormSection
	implements IResourceChangeListener {
	public static final String SECTION_TITLE = "ManifestEditor.AlertSection.title";
	public static final String KEY_NO_ALERTS = "ManifestEditor.AlertSection.noAlerts";
	public static final String KEY_UNRESOLVED = "ManifestEditor.AlertSection.unresolved";
	public static final String KEY_UNRESOLVED_TOOLTIP = "ManifestEditor.AlertSection.unresolved.tooltip";
	public static final String KEY_FRAGMENT_MARKERS = "ManifestEditor.AlertSection.fragmentMarkers";
	public static final String KEY_PLUGIN_MARKERS = "ManifestEditor.AlertSection.pluginMarkers";    
	public static final String KEY_MARKERS_TOOLTIP = "ManifestEditor.AlertSection.markers.tooltip";
	private Image alertImage;
	private Image taskAlertImage;
	private boolean updateNeeded;
	private HyperlinkHandler handler;
	private Composite container;

	class DeltaVisitor implements IResourceDeltaVisitor {
		private boolean markersChanged = false;
		private IProject ourProject;
		public DeltaVisitor(IProject ourProject) {
			this.ourProject = ourProject;
		}
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();
			if (resource == null)
				return true;
			IProject project = resource.getProject();
			if (project == null)
				return true;
			if (resource.getProject().equals(ourProject)) {
				if ((delta.getKind() | IResourceDelta.CHANGED) != 0) {
					if ((delta.getFlags() | IResourceDelta.MARKERS) != 0) {
						markersChanged = true;
						return false;
					}
				}
			} else
				return false;
			return true;
		}
		public boolean getMarkersChanged() {
			return markersChanged;
		}
	}

public AlertSection(ManifestFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	PDEPlugin.getWorkspace().addResourceChangeListener(this);
	handler = new HyperlinkHandler();
	alertImage = PDEPluginImages.DESC_ALERT_OBJ.createImage();
	taskAlertImage = PDEPluginImages.DESC_TSK_ALERT_OBJ.createImage();
}
private boolean checkMarkers(Composite parent, FormWidgetFactory factory) {
	IEditorInput input = getFormPage().getEditor().getEditorInput();
	if (!(input instanceof IFileEditorInput))
		return false;
	IFile file = ((IFileEditorInput) input).getFile();
	IProject project = file.getProject();
	try {
		final IMarker[] problems =
			project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		final IMarker[] tasks =
			project.findMarkers(IMarker.TASK, true, IResource.DEPTH_INFINITE);
		if (problems.length == 0 && tasks.length == 0)
			return false;
		final IMarker[] markers = mergeMarkers(problems, tasks);

		String [] args = { ""+problems.length, ""+tasks.length };
		String message;

		if (((ManifestEditor) getFormPage().getEditor()).isFragmentEditor())
			message = PDEPlugin.getFormattedMessage(KEY_FRAGMENT_MARKERS, args);
		else
			message = PDEPlugin.getFormattedMessage(KEY_PLUGIN_MARKERS, args);
		Label imageLabel = factory.createLabel(parent, null);
		imageLabel.setImage(taskAlertImage);
		//Label label = factory.createLabel(parent, message);
		SelectableFormLabel label = factory.createSelectableLabel(parent, message);
		label.setToolTipText(PDEPlugin.getResourceString(KEY_MARKERS_TOOLTIP));
		handler.registerHyperlink(label, new HyperlinkAdapter() {
			public void linkActivated(Control link) {
				try {
					IViewPart view = PDEPlugin.getActivePage().showView(IPageLayout.ID_TASK_LIST);
					final TaskList tasklist = (TaskList) view;
					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							tasklist.setSelection(new StructuredSelection(markers), true);
						}
					});
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	} catch (CoreException e) {
		PDEPlugin.logException(e);
		return false;
	}
	return true;
}
private boolean checkReferences(Composite parent, FormWidgetFactory factory) {
	IPluginModel model = (IPluginModel) getFormPage().getModel();
	if (model.isEditable() == false)
		return false;

	IPlugin plugin = model.getPlugin();
	IPluginImport[] imports = plugin.getImports();
	boolean unresolvedReferences = false;
	boolean cycles = false;

	for (int i = 0; i < imports.length; i++) {
		IPlugin refPlugin = PDEPlugin.getDefault().findPlugin(imports[i].getId());
		if (refPlugin == null) {
			unresolvedReferences = true;
			break;
		}
	}
	if (!unresolvedReferences) {
		DependencyLoop [] loops = DependencyLoopFinder.findLoops(plugin);
		cycles = loops.length>0;
	}
	if (!unresolvedReferences && !cycles)
		return false;
	String message = PDEPlugin.getResourceString(KEY_UNRESOLVED);
	Label imageLabel = factory.createLabel(parent, null);
	imageLabel.setImage(alertImage);
	//Label label = factory.createLabel(parent, message);
	SelectableFormLabel label = factory.createSelectableLabel(parent, message);
	label.setToolTipText(PDEPlugin.getResourceString(KEY_UNRESOLVED_TOOLTIP));
	handler.registerHyperlink(label, new HyperlinkAdapter() {
		public void linkActivated(Control link) {
			getFormPage().getEditor().showPage(ManifestEditor.DEPENDENCIES_PAGE);
		}
	});
	return true;
}
public void createAlerts(Composite container, FormWidgetFactory factory) {
	boolean hasAlerts = false;
	boolean fragment = ((ManifestEditor)getFormPage().getEditor()).isFragmentEditor();
	hasAlerts = checkMarkers(container, factory);
	if (!fragment && checkReferences(container, factory)) {
		hasAlerts = true;
	}
	if (hasAlerts == false) {
		Label label = factory.createLabel(container, PDEPlugin.getResourceString(KEY_NO_ALERTS));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.horizontalSpacing = 0;
	layout.verticalSpacing = 0;
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	handler.setBackground(factory.getBackgroundColor());
	handler.setForeground(factory.getForegroundColor());
	handler.setActiveForeground(factory.getHyperlinkColor());
	handler.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_ROLLOVER);
	
	container = factory.createComposite(parent);
	container.setLayout(layout);
	createAlerts(container, factory);
	return container;
}
public void dispose() {
	PDEPlugin.getWorkspace().removeResourceChangeListener(this);
	IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
	model.removeModelChangedListener(this);
	handler.dispose();
	alertImage.dispose();
	taskAlertImage.dispose();
	super.dispose();
}
public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase) input;
	model.addModelChangedListener(this);
}
private IMarker[] mergeMarkers(IMarker[] problems, IMarker[] tasks) {
	IMarker [] result = new IMarker [ problems.length + tasks.length ];

	int i=0;
	for (;i<problems.length; i++) {
		result[i] = problems[i];
	}
	for (int j=0; j<tasks.length; j++) {
		result[i++]= tasks[j];
	} 
	return result;
}

public void modelChanged(IModelChangedEvent e) {
	int eventType = e.getChangeType();
	if (eventType == IModelChangedEvent.WORLD_CHANGED)
	   updateNeeded = true;
	else {
		Object object = e.getChangedObjects()[0];
		if (object instanceof IPluginImport) {
			if (eventType == IModelChangedEvent.INSERT
				|| eventType == IModelChangedEvent.REMOVE) {
				updateNeeded = true;
			}
		}
	}
	if (getFormPage().isVisible())
				update();
}
/**
 *
 */
public void resourceChanged(IResourceChangeEvent event) {
	IEditorInput input = getFormPage().getEditor().getEditorInput();
	if (input instanceof IFileEditorInput) {
		IFile file = ((IFileEditorInput) input).getFile();
		IProject ourProject = file.getProject();
		
		if (event.getType()==IResourceChangeEvent.PRE_CLOSE ||
			event.getType()==IResourceChangeEvent.PRE_DELETE) {
			return;
		}

		DeltaVisitor visitor = new DeltaVisitor(ourProject);
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
		   try {
			  delta.accept(visitor);
		   } catch (CoreException e) {
			  PDEPlugin.logException(e);
		   }
		   if (visitor.getMarkersChanged()) {
			  // need to update
			  updateNeeded = true;
			  if (getFormPage().isVisible()) {
				  // need to update now!
				  container.getDisplay().asyncExec(
				    new Runnable() {
				    	public void run() {
				  		   update();
				    	}
				    }
				  );
			  }
		   }
		}
	}
}
public void update() {
	if (container.isDisposed()) return;
	if (updateNeeded) {
		Control [] children = container.getChildren();
		for (int i=0; i<children.length; i++) {
			children[i].dispose();
		}
		createAlerts(container, getFormPage().getForm().getFactory());
		container.layout(true);
		container.getParent().getParent().layout(true);
		((Composite)getFormPage().getForm().getControl()).layout(true);
		updateNeeded = false;
	}
}
}
