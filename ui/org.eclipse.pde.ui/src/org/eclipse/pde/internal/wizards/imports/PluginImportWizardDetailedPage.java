/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.base.model.plugin.*;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ViewerSorter;

public class PluginImportWizardDetailedPage extends StatusWizardPage {
	private static final String KEY_TITLE = "ImportWizard.DetailedPage.title";
	private static final String KEY_DESC = "ImportWizard.DetailedPage.desc";
	private PluginImportWizardFirstPage firstPage;
	private IPath dropLocation;
	private CheckboxTableViewer pluginListViewer;
	private Button deselectAllButton;
	private Button selectAllButton;
	private Label counterLabel;
	private static final String KEY_SELECT_ALL = "ExternalPluginsBlock.selectAll";
	private static final String KEY_DESELECT_ALL =
		"ExternalPluginsBlock.deselectAll";
	private static final String KEY_SELECTED = "ExternalPluginsBlock.selected";
	private int counter;
	private Image externalPluginImage;
	private Image externalFragmentImage;
	private Vector selected;
	private Vector models;
	private boolean loadFromRegistry;

	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}

	public class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				IPluginModelBase model = (IPluginModelBase) obj;
				return model.getPluginBase().getTranslatedName();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0) {
				if (obj instanceof IFragmentModel)
					return externalFragmentImage;
				else
					return externalPluginImage;
			}
			return null;
		}
	}

	public PluginImportWizardDetailedPage(PluginImportWizardFirstPage firstPage) {
		super("PluginImportWizardDetailedPage", false);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		externalPluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		externalFragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
		this.firstPage = firstPage;
		dropLocation = null;
		selected = new Vector();
		updateStatus(createStatus(IStatus.ERROR, ""));
	}

	private void initializeFields(IPath dropLocation) {
		loadFromRegistry = !firstPage.isOtherLocation();
		if (!dropLocation.equals(this.dropLocation)) {
			updateStatus(createStatus(IStatus.OK, ""));
			this.dropLocation = dropLocation;
			models = null;
			selected.clear();
		}
		pluginListViewer.setInput(PDEPlugin.getDefault());
		dialogChanged();
	}

	/*
	 * @see DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initializeFields(firstPage.getDropLocation());
		}
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		pluginListViewer = new CheckboxTableViewer(container, SWT.BORDER);
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(new PluginLabelProvider());
		pluginListViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				modelChecked((IPluginModelBase) event.getElement(), event.getChecked());
			}
		});

		GridData gd =
			new GridData(
				GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.widthHint = 300;

		pluginListViewer.getTable().setLayoutData(gd);

		Composite buttonContainer = new Composite(container, SWT.NONE);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.marginWidth = 0;
		buttonLayout.marginHeight = 0;
		buttonContainer.setLayout(buttonLayout);

		counterLabel = new Label(container, SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);

		selectAllButton = new Button(buttonContainer, SWT.PUSH);
		selectAllButton.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		selectAllButton.setLayoutData(gd);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll();
			}
		});

		deselectAllButton = new Button(buttonContainer, SWT.PUSH);
		deselectAllButton.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		deselectAllButton.setLayoutData(gd);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deselectAll();
			}
		});

		counterLabel = new Label(container, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		setControl(container);
	}

	public void dispose() {
		externalPluginImage.dispose();
		externalFragmentImage.dispose();
		super.dispose();
	}

	public IPluginModelBase[] getModels() {
		if (loadFromRegistry) {
			ExternalModelManager registry =
				PDEPlugin.getDefault().getExternalModelManager();
			return (IPluginModelBase[]) registry.getModels();
		}
		return new IPluginModelBase[0];
	}

	public IPluginModelBase[] getSelectedModels() {
		return (IPluginModelBase[]) selected.toArray(
			new IPluginModelBase[selected.size()]);
	}

	private IStatus validatePlugins() {
		IPluginModelBase[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(
				IStatus.ERROR,
				"No plugins found. Check the location entered on the first page. ('plugins' folder or SDK drop folder).");
		}
		if (selected.size() == 0) {
			return createStatus(IStatus.ERROR, "No plugins selected.");
		}
		return createStatus(IStatus.OK, "");
	}

	private void modelChecked(IPluginModelBase model, boolean checked) {
		if (checked) {
			selected.add(model);
			counter++;
		} else {
			selected.remove(model);
			counter--;
		}
		dialogChanged();
	}

	private void dialogChanged() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
		updateCounterLabel();
	}

	private void selectAll() {
		IPluginModelBase[] models = getModels();
		selected.clear();

		pluginListViewer.setAllChecked(true);
		for (int i = 0; i < models.length; i++) {
			selected.add(models[i]);
		}
		counter = models.length;
		dialogChanged();
	}

	private void deselectAll() {
		pluginListViewer.setAllChecked(false);
		selected.clear();
		counter = 0;
		dialogChanged();
	}

	/*
		
		private void doButtonPressed(DialogField field, int index) {
			ArrayList checked= null;
			switch (index) {
				case 3: checked= selectExistingProjects(); break;
				case 4: checked= selectLibraryProjects(); break;
				case 5: checked= selectExternalProjects(); break;
				case 7: checked= selectDependendPlugins(); break;
				default:
					return;															
			}
			for (Iterator iter= checked.iterator(); iter.hasNext();) {
				pluginList.setChecked(iter.next(), true);
			}
		}
		
		private ArrayList selectExistingProjects() {
			IWorkspaceRoot root= PDEPlugin.getWorkspace().getRoot();
			ArrayList selected= new ArrayList();
			for (int i=0; i<plugins.size(); i++) {
				IPluginModelBase curr= (IPluginModelBase)plugins.get(i);
				String id = curr.getPluginBase().getId();
				IProject proj= (IProject) root.findMember(id);
				if (proj != null) {
					selected.add(curr);
				}
			}
			return selected;	
		}
		
		private ArrayList selectLibraryProjects() {
			IWorkspaceRoot root= PDEPlugin.getWorkspace().getRoot();
			ArrayList selected= new ArrayList();
			for (int i=0; i<plugins.size(); i++) {
				IPluginModelBase curr= (IPluginModelBase) plugins.get(i);
				String id = curr.getPluginBase().getId();
				IProject proj= (IProject) root.findMember(id);
				if (proj != null && !hasSourceFolder(proj)) {
					selected.add(curr);
				}
			}
			return selected;
		}
		
		private ArrayList selectExternalProjects() {
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			ArrayList selected= new ArrayList();
			for (int i=0; i<plugins.size(); i++) {
				IPluginModelBase curr= (IPluginModelBase) plugins.get(i);
				String id = curr.getPluginBase().getId();
				IProject proj= (IProject) root.findMember(id);
				if (proj != null && !root.getLocation().isPrefixOf(proj.getLocation())) {
					selected.add(curr);
				}
			}
			return selected;
		}
	
		private boolean hasSourceFolder(IProject project) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IClasspathEntry[] entries= JavaCore.create(project).getRawClasspath();
					for (int i= 0; i < entries.length; i++) {
						if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							return true;
						}
					}
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return false;
		}	
	*/

	/*	
		private ArrayList selectDependendPlugins() {
			List plugins= fPluginsList.getElements();
			List roots= fPluginsList.getCheckedElements();
			
			HashSet checked= new HashSet();
			if (roots.size() > 1 || !((PluginModel) roots.get(0)).getId().equals("org.eclipse.core.boot")) {
				addImplicitDependencies(plugins, checked);
			}
			for (int i= 0; i < roots.size(); i++) {
				addPluginAndDependend((PluginModel) roots.get(i), checked, plugins);
			}
	
			ArrayList result= new ArrayList(checked);
			if (PluginUtil.findPlugin("org.eclipse.sdk", result) == null && PluginUtil.findPlugin("org.eclipse.ui", result) != null) {
				PluginModel sdkPlugin= PluginUtil.findPlugin("org.eclipse.sdk", plugins);
				if (sdkPlugin != null) {
					String title= "Plugin Selection";
					String message= "'org.eclipse.ui' implicitly requires 'org.eclipse.sdk'.\nOK to add 'org.eclipse.sdk' (recommended)?";
					if (MessageDialog.openQuestion(getShell(), title, message)) {
						result.add(sdkPlugin);
					}
				}
			}
			return result;
		}
	
		
		private void addImplicitDependencies(List plugins, HashSet checked) {
			PluginDescriptorModel implicit= PluginUtil.findPlugin("org.eclipse.core.boot", plugins);
			if (implicit != null) {
				checked.add(implicit);
			}		
			implicit= PluginUtil.findPlugin("org.eclipse.core.runtime", plugins);
			if (implicit != null) {
				checked.add(implicit);
			}
		}	
		
		private void addPluginAndDependend(PluginModel curr, HashSet checked, List plugins) {
			if (checked.contains(curr)) {
				return;
			}
			checked.add(curr);
			PluginPrerequisiteModel[] required= curr.getRequires();
			if (required != null) {
				for (int k= 0; k < required.length; k++) {
					String id= required[k].getPlugin();
					PluginDescriptorModel found= PluginUtil.findPlugin(id, plugins);
					if (found != null) {
						addPluginAndDependend(found, checked, plugins);
					}
				}
			}
			if (curr instanceof PluginFragmentModel) {
				String id= ((PluginFragmentModel)curr).getPlugin();
				PluginDescriptorModel found= PluginUtil.findPlugin(id, plugins);
				if (found != null) {
					addPluginAndDependend(found, checked, plugins);
				}		
			}
		}
	*/
	protected void updateCounterLabel() {
		String[] args = { "" + counter };
		String selectedLabelText = PDEPlugin.getFormattedMessage(KEY_SELECTED, args);
		counterLabel.setText(selectedLabelText);
	}
}