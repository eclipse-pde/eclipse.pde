package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.internal.junit.launcher.TestSelectionDialog;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * The JUnitPdeMainTab shows the project name, name of the test case and
 * the name of the Eclipse application name. 
 */
public class JUnitMainTab extends JUnitLaunchConfigurationTab {

	public static final String RT_WORKSPACE = "runtime-test-workspace";

	// Project UI widgets
	private Label fProjLabel;
	private Text fProjText;
	private Button fProjButton;
	private Button fKeepRunning;
	private Button fDeleteWorkspace;

	// Test class UI widgets
	private Label fTestLabel;
	private Text fTestText;
	private Button fSearchButton;
	
	// Application to launch
	private Combo fApplicationName;
	
	private final Image fTestIcon = PDEPluginImages.DESC_JUNIT_MAIN_TAB.createImage();
	private Text fWorkspaceLocation;

	
	/**
	 * @see ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns= 2;
		comp.setLayout(topLayout);		
		
		new Label(comp, SWT.NONE);
				
		createProjectControls(comp);
		createTestControls(comp);

		createSeparator(comp);
						
		createApplicationControls(comp);
		createKeepRunningControls(comp);
		createWorkspaceDataControls(comp);
	}

	public void createSeparator(Composite comp) {
		GridData gd;
		Label label= new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
	}

	public void createWorkspaceDataControls(Composite comp) {
		GridData gd;
		Label wslabel = new Label(comp, SWT.NULL);
		wslabel.setText("Workspace data:"); 
		gd = new GridData();
		gd.horizontalSpan = 2;
		wslabel.setLayoutData(gd);
		
		fWorkspaceLocation = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 1;
		fWorkspaceLocation.setLayoutData(gd); 
		fWorkspaceLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		Button browseButton = new Button(comp, SWT.PUSH);
		browseButton.setText("B&rowse...");
		setButtonGridData(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleBrowseButtonSelected();
			}
		});
		
		fDeleteWorkspace = new Button(comp, SWT.CHECK);
		fDeleteWorkspace.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		fDeleteWorkspace.setText("Clear test &workspace before launching");
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fDeleteWorkspace.setLayoutData(gd);
	}

	public void createKeepRunningControls(Composite comp) {
		GridData gd;
		fKeepRunning = new Button(comp, SWT.CHECK);
		fKeepRunning.setText("&Keep JUnit running after a test run when debugging");
		gd= new GridData();
		gd.horizontalAlignment= GridData.FILL;
		gd.horizontalSpan= 2;
		fKeepRunning.setLayoutData(gd);
		fKeepRunning.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	public void createApplicationControls(Composite comp) {
		GridData gd;
		Label appLabel = new Label(comp, SWT.NONE);
		appLabel.setText("&Application Name:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		appLabel.setLayoutData(gd);
		
		fApplicationName = new Combo(comp, SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fApplicationName.setLayoutData(gd);
		fApplicationName.setItems(JUnitLaunchConfiguration.fgApplicationNames);
		
		fApplicationName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	public void createTestControls(Composite comp) {
		GridData gd;
		fTestLabel = new Label(comp, SWT.NONE);
		fTestLabel.setText("T&est class:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fTestLabel.setLayoutData(gd);
		
		fTestText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fTestText.setLayoutData(gd);
		fTestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fSearchButton = new Button(comp, SWT.PUSH);
		fSearchButton.setText("&Search...");
		setButtonGridData(fSearchButton);
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});
	}

	public void createProjectControls(Composite comp) {
		GridData gd;
		fProjLabel = new Label(comp, SWT.NONE);
		fProjLabel.setText("&Project:");
		gd= new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		
		fProjText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fProjButton = new Button(comp, SWT.PUSH);
		fProjButton.setText("&Browse....");
		setButtonGridData(fProjButton);
		
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
	}
	
	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		updateProjectFromConfig(config);
		updateTestTypeFromConfig(config);
		updateApplicationFromConfig(config);
		updateKeepRunning(config);
		updateDeleteWorkspace(config);
		updateWorkspaceLocation(config);
	}
	
	private void updateWorkspaceLocation(ILaunchConfiguration config) {
		String workspaceLocation= "";
		try {
			workspaceLocation = config.getAttribute(ILauncherSettings.LOCATION, "");
		} catch (CoreException ce) {
		}
		fWorkspaceLocation.setText(workspaceLocation);
	}
	
	private void updateKeepRunning(ILaunchConfiguration config) {
		boolean running= false;
		try {
			running= config.getAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, false);
		} catch (CoreException ce) {
		}
		fKeepRunning.setSelection(running);	 	
	}

	private void updateDeleteWorkspace(ILaunchConfiguration config) {
		boolean delete = true;
		try {
			delete = config.getAttribute(ILauncherSettings.DOCLEAR, true);
		} catch (CoreException ce) {
		}
		fDeleteWorkspace.setSelection(delete);	 	
	}
	
	protected void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName= "";
		try {
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		} catch (CoreException ce) {
		}
		fProjText.setText(projectName);
	}
	
	protected void updateTestTypeFromConfig(ILaunchConfiguration config) {
		String testTypeName= "";
		try {
			testTypeName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
		} catch (CoreException ce) {			
		}
		fTestText.setText(testTypeName);		
	}

	protected void updateApplicationFromConfig(ILaunchConfiguration config) {
		try {
			String appName = config.getAttribute(
				ILauncherSettings.APPLICATION, 
				JUnitLaunchConfiguration.fgDefaultApp);
			selectApplicationName(appName);
		} catch (CoreException ce) {			
		}
	}
	
	private void selectApplicationName(String name) {
		String[] items= fApplicationName.getItems();
		for (int i= 0; i < items.length; i++) {
			if (items[i].equals(name)) {
				fApplicationName.select(i);
				break;
			}
		}
	}
	
	/**
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)fProjText.getText());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)fTestText.getText());
		String appName= fApplicationName.getItem(fApplicationName.getSelectionIndex());
		config.setAttribute(ILauncherSettings.APPLICATION, appName);
		config.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, fKeepRunning.getSelection());
		config.setAttribute(ILauncherSettings.DOCLEAR, fDeleteWorkspace.getSelection());
		config.setAttribute(ILauncherSettings.LOCATION, fWorkspaceLocation.getText());
	}

	/**
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		super.dispose();
		fTestIcon.dispose();
	}
		
	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {
		Shell shell = getShell();
		
		IJavaProject javaProject = getJavaProject();
		
		//TO DO should not use the workbenchWindow as the runnable context
		SelectionDialog dialog = new TestSelectionDialog(shell, getLaunchConfigurationDialog(), javaProject);
		dialog.setTitle("Test Selection");
		dialog.setMessage("Choose a test case or test suite:");
		if (dialog.open() == SelectionDialog.CANCEL) {
			return;
		}
		
		Object[] results = dialog.getResult();
		if ((results == null) || (results.length < 1)) {
			return;
		}		
		IType type = (IType)results[0];
		fTestText.setText(type.getFullyQualifiedName());
		javaProject = type.getJavaProject();
		fProjText.setText(javaProject.getElementName());
	}

	protected void handleBrowseButtonSelected() {
		IPath chosen = chooseWorkspaceLocation();
		if (chosen != null) {
			fWorkspaceLocation.setText(chosen.toOSString());
			//TODO updateStatus();
		}
	}

	/**
	 * Browses for a workbench location.
	 */
	private IPath chooseWorkspaceLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		dialog.setFilterPath(fWorkspaceLocation.getText());
		dialog.setText("Workspace Location");
		dialog.setMessage("Select Workspace Location");
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	/**
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected() {
		IJavaProject project = chooseJavaProject();
		if (project == null) {
			return;
		}
		
		String projectName = project.getElementName();
		fProjText.setText(projectName);		
	}
	
	/**
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	protected IJavaProject chooseJavaProject() {
		IJavaProject[] projects;
		try {
			projects= JavaCore.create(getWorkspaceRoot()).getJavaProjects();
		} catch (JavaModelException e) {
			PDEPlugin.log(e.getStatus());
			projects= new IJavaProject[0];
		}
		
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Project Selection");
		dialog.setMessage("Choose a project to constrain the search for main types:");
		dialog.setElements(projects);
		
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			dialog.setInitialSelections(new Object[] { javaProject });
		}
		if (dialog.open() == ElementListSelectionDialog.OK) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		return null;		
	}
	
	/**
	 * Return the IJavaProject corresponding to the project name in the project name
	 * text field, or null if the text does not match a project name.
	 */
	protected IJavaProject getJavaProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getJavaModel().getJavaProject(projectName);		
	}
	
	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/**
	 * Convenience method to get access to the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);
		
		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
				setErrorMessage("Project does not exist.");
				return false;
			}
		}

		name = fTestText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage("Test not specified.");
			return false;
		}
		
		String workspace = fWorkspaceLocation.getText().trim();
		if (workspace.length() == 0) {
			setErrorMessage("Workspace location not specified.");
			return false;
		}

		return true;
	}
	
	/**
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		} else {
			// We set empty attributes for project & main type so that when one config is
			// compared to another, the existence of empty attributes doesn't cause an
			// incorrect result (the performApply() method can result in empty values
			// for these attributes being set on a config if there is nothing in the
			// corresponding text boxes)
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		}
		initializeTestTypeAndName(javaElement, config);
		config.setAttribute(ILauncherSettings.LOCATION, JUnitLaunchConfiguration.getDefaultWorkspace());
	}
	
	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	protected void initializeTestTypeAndName(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name= "";
		try {
			// we only do a search for compilation units or class files or 
			// or source references
			if ((javaElement instanceof ICompilationUnit) || 
				(javaElement instanceof ISourceReference) ||
				(javaElement instanceof IClassFile)) {
		
				IType[] types = TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), new Object[] {javaElement});
				if ((types == null) || (types.length < 1)) {
					return;
				}
				// Simply grab the first main type found in the searched element
				name = types[0].getFullyQualifiedName();
			}	
		} catch (InterruptedException ie) {
		} catch (InvocationTargetException ite) {
		}		
		if (name == null)
			name= "";
				
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
		if (name.length() > 0) {
			int index = name.lastIndexOf('.');
			if (index > 0) {
				name = name.substring(index + 1);
			}
			name = getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}	
	/**
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Plugin Test";
	}
	
	/**
	 * @see ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fTestIcon;
	}	
}
