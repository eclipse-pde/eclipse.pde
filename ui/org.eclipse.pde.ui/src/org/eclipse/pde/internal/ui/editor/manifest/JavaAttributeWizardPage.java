package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.*;
import java.lang.reflect.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.build.*;

public class JavaAttributeWizardPage extends WizardPage {
	public static final String PAGE_TITLE = "JavaAttributeWizard.title";
	public static final String DUPLICATION_TITLE =
		"JavaAttributeWizard.duplication.title";
	public static final String KEY_GENERATING = "JavaAttributeWizard.generating";
	public static final String KEY_FINISH = "JavaAttributeWizard.finish";
	public static final String DUPLICATION_MESSAGE =
		"JavaAttributeWizard.duplication.message";
	public static final String PAGE_DESC = "JavaAttributeWizard.desc";
	public static final String KEY_CONTAINER_SELECTION =
		"JavaAttributeWizard.containerSelection";
	public static final String KEY_PACKAGE_SELECTION =
		"JavaAttributeWizard.packageSelection";
	public static final String KEY_MISSING_CONTAINER =
		"JavaAttributeWizard.error.container";

	public static final String GENERATE = "JavaAttributeWizard.generate";
	public static final String GENERATE_CONTAINER_NAME =
		"JavaAttributeWizard.generate.container";
	public static final String GENERATE_CONTAINER_BROWSE =
		"JavaAttributeWizard.generate.container.browse";
	public static final String GENERATE_PACKAGE_NAME =
		"JavaAttributeWizard.generate.package";
	public static final String GENERATE_PACKAGE_BROWSE =
		"JavaAttributeWizard.generate.package.browse";
	public static final String GENERATE_CLASS_NAME = "JavaAttributeWizard.generate.class";
	public static final String GENERATE_OPEN_EDITOR =
		"JavaAttributeWizard.generate.openEditor";

	public static final String SEARCH = "JavaAttributeWizard.search";
	public static final String SEARCH_CLASS_NAME = "JavaAttributeWizard.search.className";
	public static final String SEARCH_CLASS_BROWSE =
		"JavaAttributeWizard.search.className.browse";

	public static final String KEY_FIND_TYPE = "JavaAttributeWizard.findType";
	public static final String KEY_FILTER = "JavaAttributeWizard.filter";
	private String className;
	private IProject project;
	private ISchemaAttribute attInfo;
	
	private Button searchButton;
	private Label searchLabel;
	private Text searchText;
	private Button searchBrowse;
	
	private Button generateButton;
	private Label containerLabel;
	private Text containerText;
	private Button containerBrowse;
	
	private Label packageLabel;
	private Text packageText;
	private Button packageBrowse;
	
	private Label classLabel;
	private Text classText;
	
	private Button openFileButton;

	private ModifyListener modifyListener;
	private IPluginModelBase model;

	public JavaAttributeWizardPage(
		IProject project,
		IPluginModelBase model,
		ISchemaAttribute attInfo,
		String className) {
		super("classPage");
		this.project = project;
		this.model = model;
		this.attInfo = attInfo;
		this.className = className;
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
		modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		};
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 5;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createSearchSection(container);
		createGenerateSection(container);
		
		enableGenerateSection(false);
		
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.JAVA_ATTRIBUTE_WIZARD_PAGE);
	}
	
	private void enableGenerateSection(boolean enabled) {
		containerLabel.setEnabled(enabled);
		containerText.setEnabled(enabled);
		containerBrowse.setEnabled(enabled);
		packageLabel.setEnabled(enabled);
		packageText.setEnabled(enabled);
		packageBrowse.setEnabled(enabled);
		classLabel.setEnabled(enabled);
		classText.setEnabled(enabled);
		openFileButton.setEnabled(enabled);
	}

	private void enableSearchSection(boolean enabled) {
		searchLabel.setEnabled(enabled);
		searchText.setEnabled(enabled);
		searchBrowse.setEnabled(enabled);
	}

	private void createSearchSection(Composite parent) {
		searchButton = new Button(parent, SWT.RADIO);
		searchButton.setText(PDEPlugin.getResourceString(SEARCH));
		searchButton.setSelection(true);
		searchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				generateButton.setSelection(!searchButton.getSelection());
				enableSearchSection(searchButton.getSelection());
				enableGenerateSection(!searchButton.getSelection());
				verifyComplete();
			}


		});
		
		Composite searchSection = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		searchSection.setLayout(layout);
		searchSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		searchLabel = new Label(searchSection, SWT.NONE);
		searchLabel.setText(PDEPlugin.getResourceString(SEARCH_CLASS_NAME));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		searchLabel.setLayoutData(gd);
		
		searchText = new Text(searchSection, SWT.BORDER);
		searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		searchText.setText(className);
		searchText.addModifyListener(modifyListener);

		searchBrowse = new Button(searchSection, SWT.PUSH);
		searchBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(searchBrowse);
		searchBrowse.setText(PDEPlugin.getResourceString(SEARCH_CLASS_BROWSE));
		searchBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFindType(searchText);
			}
		});
	}

	private void createGenerateSection(Composite parent) {
		generateButton = new Button(parent, SWT.RADIO);
		generateButton.setText(PDEPlugin.getResourceString(GENERATE));
		generateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				searchButton.setSelection(!generateButton.getSelection());
				enableGenerateSection(generateButton.getSelection());
				enableSearchSection(!generateButton.getSelection());
				verifyComplete();
			}
		});
		
		Composite generateSection = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		generateSection.setLayout(layout);
		generateSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		containerLabel = new Label(generateSection, SWT.NONE);
		containerLabel.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_NAME));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		containerLabel.setLayoutData(gd);
		
		containerText = new Text(generateSection, SWT.BORDER);
		containerText.setText(computeSourceContainer());
		containerText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		containerText.addModifyListener(modifyListener);

		containerBrowse = new Button(generateSection, SWT.PUSH);
		containerBrowse.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_BROWSE));
		containerBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(containerBrowse);
		containerBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFindContainer(containerText);
			}
		});

		packageLabel = new Label(generateSection, SWT.NONE);
		packageLabel.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		packageLabel.setLayoutData(gd);
		
		packageText = new Text(generateSection, SWT.BORDER);
		packageText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		packageText.addModifyListener(modifyListener);
		
		packageBrowse = new Button(generateSection, SWT.PUSH);
		packageBrowse.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_BROWSE));
		packageBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));		
		SWTUtil.setButtonDimensionHint(packageBrowse);
		packageBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFindPackage(packageText);
			}
		});

		classLabel = new Label(generateSection, SWT.NONE);
		classLabel.setText(PDEPlugin.getResourceString(GENERATE_CLASS_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		classLabel.setLayoutData(gd);
		
		classText = new Text(generateSection, SWT.BORDER);
		classText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		classText.addModifyListener(modifyListener);

		int loc = className.lastIndexOf('.');
		if (loc != -1) {
			packageText.setText(className.substring(0, loc));
			classText.setText(className.substring(loc + 1));
		}

		openFileButton = new Button(generateSection, SWT.CHECK);
		openFileButton.setText(PDEPlugin.getResourceString(GENERATE_OPEN_EDITOR));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 25;
		openFileButton.setLayoutData(gd);
		openFileButton.setSelection(true);

	}
	
	

	private String computeSourceContainer() {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null || buildModel.isLoaded() == false)
			return "";
		String candidate = null;
		IBuildEntry[] entries = buildModel.getBuild().getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			IBuildEntry entry = entries[i];
			if (entry.getName().startsWith("source.") == false)
				continue;
			if (candidate != null) {
				// more than one folder - abort
				candidate = null;
				break;
			}
			String[] tokens = entry.getTokens();
			if (tokens.length > 1) {
				candidate = null;
				break;
			}
			candidate = tokens[0];
		}
		return candidate != null ? candidate : "";
	}

	private void createTask(IFile file) {
		String message = PDEPlugin.getFormattedMessage(KEY_FINISH, file.getName());
		try {
			IMarker marker = file.createMarker(IMarker.TASK);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
			marker.setAttribute(IMarker.MESSAGE, message);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean finish() {
		if (searchButton.getSelection()) {
			className = searchText.getText();
		} else {
			className = packageText.getText() + "." + classText.getText();
			return generateClass();
		}
		return true;
	}
	private boolean generateClass() {
		if (isAlreadyCreated(className)) {
			Display.getCurrent().beep();
			boolean ok =
				MessageDialog.openQuestion(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(DUPLICATION_TITLE),
					PDEPlugin.getFormattedMessage(DUPLICATION_MESSAGE, className));
			if (!ok)
				return false;
		}
		final String folderName = containerText.getText();
		final boolean openFile = openFileButton.getSelection();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				try {
					generateClass(folderName, openFile, pm);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		};
		try {
			getContainer().run(false, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}
	private void generateClass(
		String folderName,
		boolean openFile,
		IProgressMonitor monitor)
		throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);

		IFolder folder = getSourceFolder(folderName);

		AttributeClassCodeGenerator generator =
			new AttributeClassCodeGenerator(javaProject, folder, className, attInfo);
		monitor.subTask(PDEPlugin.getFormattedMessage(KEY_GENERATING, className));
		IFile file = generator.generate(monitor);
		if (file != null) {
			//Using Java todo support instead 
			//createTask(file);
			if (openFile) {
				IWorkbenchPage page = PDEPlugin.getActivePage();
				page.openEditor(file);
			}
		}
		monitor.done();
	}
	public String getClassName() {
		return className;
	}
	private IFolder getSourceFolder(String folderName) throws CoreException {
		IPath path = project.getFullPath().append(folderName);
		IFolder folder = project.getWorkspace().getRoot().getFolder(path);
		return folder;
	}
	private void handleFindContainer(Text target) {
		ContainerSelectionDialog dialog =
			new ContainerSelectionDialog(
				target.getShell(),
				project,
				false,
				PDEPlugin.getResourceString(KEY_CONTAINER_SELECTION));
		int status = dialog.open();
		if (status == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				IPath path = (IPath) result[0];
				target.setText(path.lastSegment());
			}
		}
	}
	private void handleFindPackage(Text target) {
		try {
			SelectionDialog dialog =
				JavaUI.createPackageDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					JavaCore.create(project),
					0);
			dialog.setTitle(PDEPlugin.getResourceString(KEY_PACKAGE_SELECTION));
			dialog.setMessage("");
			int status = dialog.open();
			if (status == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				IPackageFragment packageFragment = (IPackageFragment) result[0];
				target.setText(packageFragment.getElementName());
				target.setFocus();
				target.selectAll();
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleFindType(Text target) {
		try {
			SelectionDialog dialog =
				JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					getContainer(),
					project,
					IJavaElementSearchConstants.CONSIDER_TYPES,
					false);
			dialog.setTitle(PDEPlugin.getResourceString(KEY_FIND_TYPE));
			dialog.setMessage(PDEPlugin.getResourceString(KEY_FILTER));
			int status = dialog.open();
			if (status == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				IType type = (IType) result[0];
				target.setText(type.getFullyQualifiedName());
				target.setFocus();
				target.selectAll();
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean isAlreadyCreated(String fullName) {
		IJavaProject javaProject = JavaCore.create(project);

		IPath path = new Path(fullName.replace('.', '/') + ".java");
		try {
			IJavaElement element = javaProject.findElement(path);
			return element != null;
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}
	
	private void verifyComplete() {
		IStatus status = null;
		if (searchButton.getSelection()) {
			status = JavaConventions.validateJavaTypeName(searchText.getText());
		} else {
			status = JavaConventions.validatePackageName(packageText.getText());
			IStatus second = JavaConventions.validateJavaTypeName(classText.getText());
			if (second.getSeverity() > status.getSeverity())
				status = second;
		}
		setPageComplete(
			status.getSeverity() != IStatus.ERROR
				&& containerText.getText().length() > 0);

		String errorMessage = null;
		if (status.getSeverity() == IStatus.ERROR)
			errorMessage = status.getMessage();
		if (errorMessage == null && containerText.getText().length() == 0)
			errorMessage = PDEPlugin.getResourceString(KEY_MISSING_CONTAINER);

		if (errorMessage != null)
			setErrorMessage(errorMessage);
		else 
			setErrorMessage(null);
		
		if (status.getSeverity() != IStatus.OK)
			setMessage(status.getMessage());
		else 
			setMessage(null);
	}
}
