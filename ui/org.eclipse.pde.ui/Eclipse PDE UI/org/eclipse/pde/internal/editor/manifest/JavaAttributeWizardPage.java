package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.ui.*;
import java.lang.reflect.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.internal.codegen.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.core.resources.*;
import java.util.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.events.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.jface.dialogs.*;

public class JavaAttributeWizardPage extends WizardPage {
	public static final String PAGE_TITLE = "JavaAttributeWizard.title";
	public static final String DUPLICATION_TITLE = "JavaAttributeWizard.duplication.title";
	public static final String KEY_GENERATING = "JavaAttributeWizard.generating";
	public static final String KEY_FINISH = "JavaAttributeWizard.finish";
	public static final String DUPLICATION_MESSAGE = "JavaAttributeWizard.duplication.message";
	public static final String PAGE_DESC = "JavaAttributeWizard.desc";
	public static final String KEY_CONTAINER_SELECTION = "JavaAttributeWizard.containerSelection";
	public static final String KEY_MISSING_CLASS = "JavaAttributeWizard.error.class";
	public static final String KEY_MISSING_CONTAINER = "JavaAttributeWizard.error.container";
	public static final String KEY_MISSING_PACKAGE = "JavaAttributeWizard.error.package";

	public static final String GENERATE = "JavaAttributeWizard.generate";
	public static final String GENERATE_CONTAINER_NAME = "JavaAttributeWizard.generate.container";
	public static final String GENERATE_CONTAINER_BROWSE = "JavaAttributeWizard.generate.container.browse";
	public static final String GENERATE_PACKAGE_NAME = "JavaAttributeWizard.generate.package";
	public static final String GENERATE_PACKAGE_BROWSE = "JavaAttributeWizard.generate.package.browse";
	public static final String GENERATE_CLASS_NAME = "JavaAttributeWizard.generate.class";
	public static final String GENERATE_OPEN_EDITOR = "JavaAttributeWizard.generate.openEditor";

	public static final String SEARCH = "JavaAttributeWizard.search";
	public static final String SEARCH_CLASS_NAME = "JavaAttributeWizard.search.className";
	public static final String SEARCH_CLASS_BROWSE = "JavaAttributeWizard.search.className.browse";
		
	private String className;
	private IProject project;
	private ISchemaAttribute attInfo;
	private PageBook pageBook;
	private Button searchButton;
	private Button generateButton;
	private Text searchText;
	private Text containerText;
	private Button openFileButton;
	private Text packageText;
	private Text classText;
	private Button requiredMethodsButton;
	private Composite searchPage;
	private Composite generatePage;
	private ModifyListener modifyListener;

public JavaAttributeWizardPage(IProject project, ISchemaAttribute attInfo, String className) {
	super("classPage");
	this.project = project;
	this.attInfo = attInfo;
	this.className = className;
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	modifyListener = new ModifyListener () {
		public void modifyText(ModifyEvent e) {
			verifyComplete();
		}
	};
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.marginWidth = 10;
	layout.marginHeight = 10;
	layout.verticalSpacing = 9;
	container.setLayout(layout);
	GridData gd = new GridData(GridData.FILL_BOTH);
	container.setLayoutData(gd);

	searchButton = new Button(container, SWT.RADIO);
	searchButton.setText(PDEPlugin.getResourceString(SEARCH));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	searchButton.setLayoutData(gd);
	searchButton.setSelection(true);
	searchButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			pageBook.showPage(searchPage);
			verifyComplete();
		}
	});

	generateButton = new Button(container, SWT.RADIO);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	generateButton.setText(PDEPlugin.getResourceString(GENERATE));
	generateButton.setLayoutData(gd);
	generateButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			pageBook.showPage(generatePage);
			verifyComplete();
		}
	});
/*
	gd = new GridData(GridData.FILL_HORIZONTAL);
	Label label =
		new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.SHADOW_OUT);
	label.setLayoutData(gd);
*/

	pageBook = new PageBook(container, SWT.NULL);
	gd = new GridData(GridData.FILL_BOTH);
	pageBook.setLayoutData(gd);
	createSearchPage();
	createGeneratePage();
	setControl(container);
}
private void createGeneratePage() {
	Composite page = new Composite(pageBook, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 3;
	//layout.horizontalSpacing = 9;
	layout.verticalSpacing = 9;
	page.setLayout(layout);

	Label label = new Label(page, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_NAME));
	containerText = new Text(page, SWT.BORDER);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	containerText.setLayoutData(gd);
	containerText.addModifyListener(modifyListener);

	Button button = new Button(page, SWT.PUSH);
	button.setText(PDEPlugin.getResourceString(GENERATE_CONTAINER_BROWSE));
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleFindContainer(containerText);
		}
	});

	label = new Label(page, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_NAME));
	packageText = new Text(page, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	packageText.setLayoutData(gd);
	packageText.addModifyListener(modifyListener);
	button = new Button(page, SWT.PUSH);
	button.setText(PDEPlugin.getResourceString(GENERATE_PACKAGE_BROWSE));
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleFindPackage(packageText);
		}
	});

	label = new Label(page, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(GENERATE_CLASS_NAME));
	classText = new Text(page, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	classText.setLayoutData(gd);
	classText.addModifyListener(modifyListener);

	new Label(page, SWT.NONE);

	int loc = className.lastIndexOf('.');
	if (loc!= -1) {
		packageText.setText(className.substring(0, loc));
		classText.setText(className.substring(loc+1));
	}

	openFileButton = new Button(page, SWT.CHECK);
	openFileButton.setText(PDEPlugin.getResourceString(GENERATE_OPEN_EDITOR));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 3;
	openFileButton.setLayoutData(gd);
	openFileButton.setSelection(true);
	generatePage = page;
}
private void createSearchPage() {
	Composite page = new Composite(pageBook, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 3;
	//layout.horizontalSpacing = 9;
	layout.verticalSpacing = 9;
	page.setLayout(layout);

	Label label = new Label(page, SWT.NONE);
	label.setText(PDEPlugin.getResourceString(SEARCH_CLASS_NAME));
	searchText = new Text(page, SWT.BORDER);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	searchText.setLayoutData(gd);
	searchText.setText(className);
	searchText.addModifyListener(modifyListener);

	Button button = new Button(page, SWT.PUSH);
	button.setText(PDEPlugin.getResourceString(SEARCH_CLASS_BROWSE));
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleFindType(searchText);
		}
	});
	
	pageBook.showPage(page);
	searchPage = page;
	verifyComplete();
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
	}
	else {
		className = packageText.getText()+"."+classText.getText();
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
		createTask(file);
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
			JavaUI.createPackageDialog(PDEPlugin.getActiveWorkbenchShell(), JavaCore.create(project), 0);
		int status = dialog.open();
		if (status == SelectionDialog.OK) {
			Object[] result = dialog.getResult();
			IPackageFragment packageFragment = (IPackageFragment)result[0];
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
	boolean complete;
	String errorMessage = null;
	if (searchButton.getSelection()) {
		complete = searchText.getText().length() > 0;
		if (!complete) {
			errorMessage = PDEPlugin.getResourceString(KEY_MISSING_CLASS);
		}
	} else {
		complete = containerText.getText().length() > 0;
		if (!complete)
			errorMessage = PDEPlugin.getResourceString(KEY_MISSING_CONTAINER);
		else {
			complete = packageText.getText().length() > 0;
			if (!complete)
				errorMessage = PDEPlugin.getResourceString(KEY_MISSING_PACKAGE);
			else {
				complete = classText.getText().length() > 0;
				if (!complete)
					errorMessage = PDEPlugin.getResourceString(KEY_MISSING_CLASS);
			}
		}
	}
	setPageComplete(complete);
	setErrorMessage(errorMessage);
}
}
