/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.ui.*;
import java.lang.reflect.*;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.core.plugin.*;

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
	public static final String NESTED_TITLE = "JavaAttributeWizard.nested.title";
	public static final String NESTED_DESC = "JavaAttributeWizard.nested.desc";

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
	private static final int MAX_WIDTH = 250;
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

	private IPackageFragmentRoot[] sourceFolders;
	
	class ContentProvider extends WorkbenchContentProvider {
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

	public JavaAttributeWizardPage(
		IProject project,
		IPluginModelBase model,
		ISchemaAttribute attInfo,
		String className) {
		super("classPage");
		this.project = project;
		this.attInfo = attInfo;
		this.className = className;
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
		modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		};
		//initialize
		computeSourceFolders();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 5;
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		setPageComplete(false);

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

		//		public void run(IAction action) {
		//		   Dialog sdialog = new ContainerDialog(searchText.getShell(),
		//			  null);
		//		   sdialog.open();
		//		}

		searchButton = new Button(parent, SWT.RADIO);
		searchButton.setText(PDEPlugin.getResourceString(SEARCH));
		searchButton.setSelection(true);
		searchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableSearchSection(searchButton.getSelection());
				verifyComplete();
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		searchButton.setLayoutData(gd);

		searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText(PDEPlugin.getResourceString(SEARCH_CLASS_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		searchLabel.setLayoutData(gd);

		searchText = new Text(parent, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = MAX_WIDTH;
		searchText.setLayoutData(gd);
		searchText.setText(className);

		searchText.addModifyListener(modifyListener);

		searchBrowse = new Button(parent, SWT.PUSH);
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
				enableGenerateSection(generateButton.getSelection());
				verifyComplete();
			}
		});

		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		generateButton.setLayoutData(gd);

		containerLabel = new Label(parent, SWT.NONE);
		containerLabel.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		containerLabel.setLayoutData(gd);

		containerText = new Text(parent, SWT.BORDER);
		containerText.setText(
			(sourceFolders.length > 0)
				? sourceFolders[0].getElementName() + Path.SEPARATOR
				: "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = MAX_WIDTH;
		containerText.setLayoutData(gd);
		containerText.addModifyListener(modifyListener);

		containerBrowse = new Button(parent, SWT.PUSH);
		containerBrowse.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_BROWSE));
		containerBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(containerBrowse);
		containerBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFindContainer();
			}
		});

		packageLabel = new Label(parent, SWT.NONE);
		packageLabel.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		packageLabel.setLayoutData(gd);

		packageText = new Text(parent, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = MAX_WIDTH;
		packageText.setLayoutData(gd);
		packageText.addModifyListener(modifyListener);

		packageBrowse = new Button(parent, SWT.PUSH);
		packageBrowse.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_BROWSE));
		packageBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(packageBrowse);
		packageBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFindPackage();
			}
		});

		classLabel = new Label(parent, SWT.NONE);
		classLabel.setText(PDEPlugin.getResourceString(GENERATE_CLASS_NAME));
		gd = new GridData();
		gd.horizontalIndent = 25;
		classLabel.setLayoutData(gd);

		classText = new Text(parent, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = MAX_WIDTH;
		classText.setLayoutData(gd);
		classText.addModifyListener(modifyListener);

		int loc = className.lastIndexOf('.');
		if (loc != -1) {
			packageText.setText(className.substring(0, loc));
			classText.setText(className.substring(loc + 1));
		}

		openFileButton = new Button(parent, SWT.CHECK);
		openFileButton.setText(PDEPlugin.getResourceString(GENERATE_OPEN_EDITOR));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 25;
		openFileButton.setLayoutData(gd);
		openFileButton.setSelection(true);

	}

	private void computeSourceFolders() {
		ArrayList folders = new ArrayList();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						folders.add(roots[i]);
					}
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		sourceFolders =
			(IPackageFragmentRoot[]) folders.toArray(
				new IPackageFragmentRoot[folders.size()]);
	}

	private IPackageFragmentRoot getSourceFolder(String folderName) {
		if (folderName != null) {
			for (int i = 0; i < sourceFolders.length; i++) {
				if (sourceFolders[i]
					.getPath()
					.removeFirstSegments(1)
					.equals(new Path(folderName))) {
					return sourceFolders[i];
				}
			}
		}
		return null;
	}

	protected static void addSourceFolder(String name, IProject project)
		throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path);
	}

	private static void ensureFolderExists(IProject project, IPath folderPath)
		throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
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

		int separatorIndex = folderName.lastIndexOf(Path.SEPARATOR);
		if ((separatorIndex != -1
			&& separatorIndex != (folderName.length() - 1)
			&& separatorIndex != 0)
			|| (folderName.indexOf(Path.SEPARATOR, 1) != -1
				&& folderName.indexOf(Path.SEPARATOR, 1) != separatorIndex)) {
			MessageDialog.openError(
				searchText.getShell(),
				PDEPlugin.getResourceString(NESTED_TITLE),
				PDEPlugin.getFormattedMessage(NESTED_DESC, folderName));
			return false;
		}

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

		if (folderName.charAt(0) == Path.SEPARATOR)
			folderName = folderName.substring(1, folderName.length());

		IJavaProject javaProject = JavaCore.create(project);
		IPath path = project.getFullPath().append(folderName);
		IFolder folder = project.getWorkspace().getRoot().getFolder(path);
		AttributeClassCodeGenerator generator =
			new AttributeClassCodeGenerator(javaProject, folder, className, attInfo);
		monitor.subTask(PDEPlugin.getFormattedMessage(KEY_GENERATING, className));

		path = project.getFullPath().append(folderName);

		if (!path.toFile().exists()) {
			// begin insert for source file conversion
			IPackageFragmentRoot sourceFolder = getSourceFolder(folderName);
			if (sourceFolder == null) {
				try {
					IClasspathEntry newSrcEntry =
						JavaCore.newSourceEntry(folder.getFullPath());
					IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
					IClasspathEntry[] newEntries =
						new IClasspathEntry[oldEntries.length + 1];
					System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
					newEntries[oldEntries.length] = newSrcEntry;
					javaProject.setRawClasspath(newEntries, monitor);

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			// end insert
		}

		addSourceFolder(folderName, project);

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

	private void handleFindContainer() {
		FolderSelectionDialog dialog =
			new FolderSelectionDialog(
				getContainer().getShell(),
				new WorkbenchLabelProvider(),
				new ContentProvider() {
		});
		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					return ((IProject)element).equals(project);
				}
				return element instanceof IFolder;
			}			
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString(KEY_CONTAINER_SELECTION));
		int status = dialog.open();
		if (status == FolderSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (!(result[0] instanceof IFolder))
				return;
			IFolder folder = (IFolder) result[0];
			containerText.setText(
				folder.getProjectRelativePath().addTrailingSeparator().toString());
			containerBrowse.setFocus();

		}
	}

	private void handleFindPackage() {
		try {
			SelectionDialog dialog;

			IPackageFragmentRoot sourceFolder = getSourceFolder(containerText.getText());

			if (sourceFolder != null) {
				dialog =
					JavaUI.createPackageDialog(
						getContainer().getShell(),
						sourceFolder);
			} else {
				dialog =
					JavaUI.createPackageDialog(
						getContainer().getShell(),
						JavaCore.create(project),
						0,
						"");
			}
			dialog.setTitle(PDEPlugin.getResourceString(KEY_PACKAGE_SELECTION));
			dialog.setMessage("");
			int status = dialog.open();
			if (status == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				IPackageFragment packageFragment = (IPackageFragment) result[0];
				sourceFolder = (IPackageFragmentRoot) packageFragment.getParent();
				if (sourceFolder != null) {
					containerText.setText(
						sourceFolder
							.getPath()
							.removeFirstSegments(1)
							.addTrailingSeparator()
							.toString());
				} else {
					containerText.setText("");
				}
				packageText.setText(packageFragment.getElementName());
				packageBrowse.setFocus();
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}

	}

	private void handleFindType(Text target) {
		boolean initSearchSelect = searchButton.getSelection();
		try {
			SelectionDialog dialog =
				JavaUI.createTypeDialog(
					getContainer().getShell(),
					getContainer(),
					project,
					IJavaElementSearchConstants.CONSIDER_TYPES,
					false);
			dialog.setTitle(PDEPlugin.getResourceString(KEY_FIND_TYPE));
			dialog.setMessage(PDEPlugin.getResourceString(KEY_FILTER));
			int status = dialog.open();
			//ensure re-indexing does not reset selection
			searchButton.setSelection(initSearchSelect);
			generateButton.setSelection(!initSearchSelect);
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
			return javaProject.findElement(path) != null;
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}

	private void verifyComplete() {
		IStatus status = null;
		if (searchButton.getSelection()) {
			status = JavaConventions.validateJavaTypeName(searchText.getText());
			setPageComplete(status.getSeverity() != IStatus.ERROR);
		} else {
			status = JavaConventions.validatePackageName(packageText.getText());
			IStatus second = JavaConventions.validateJavaTypeName(classText.getText());

			if (second.getSeverity() > status.getSeverity())
				status = second;

			setPageComplete(
				status.getSeverity() != IStatus.ERROR
					&& containerText.getText().length() > 0);
		}

		String errorMessage = null;
		if (status.getSeverity() == IStatus.ERROR)
			errorMessage = status.getMessage();
		if (errorMessage == null
			&& !searchButton.getSelection()
			&& containerText.getText().length() == 0)
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
