package org.eclipse.pde.internal.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * Insert the type's description here.
 * @see WizardPage
 */
public class RequiredPluginsContainerPage extends WizardPage implements IClasspathContainerPage {
	private IClasspathEntry entry;
	private CheckboxTableViewer viewer;
	private Image projectImage;
	private Image libraryImage;
	private IClasspathEntry [] realEntries;
	
	class EntryContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object [] getElements(Object parent) {
			if (realEntries!=null) return realEntries;
			return new Object[0];
		}
	}
	
	class EntrySorter extends ViewerSorter {
		public int category(Object obj) {
			IClasspathEntry entry = (IClasspathEntry)obj;
			return entry.getEntryKind()==IClasspathEntry.CPE_PROJECT ? 
				-10 : 0;
		}
	}
	
	class EntryLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getText(Object obj) {
			IClasspathEntry entry = (IClasspathEntry)obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return entry.getPath().segment(0);
			else
				return entry.getPath().toOSString();
		}
		
		public Image getImage(Object obj) {
			IClasspathEntry entry = (IClasspathEntry)obj;
			int kind = entry.getEntryKind();
			if (kind==IClasspathEntry.CPE_PROJECT)
				return projectImage;
			else if (kind==IClasspathEntry.CPE_LIBRARY)
				return libraryImage;
			return null;
		}
		public String getColumnText(Object obj, int col) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int col) {
			return getImage(obj);
		}
	}
	/**
	 * The constructor.
	 */
	public RequiredPluginsContainerPage() {
		super("requiredPluginsContainerPage");
		setTitle("Required Plug-in Entries");
		setDescription("This container dynamically manages entries for the required plug-ins.");
		projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
		libraryImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
		setImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
	}
	
	public void dispose() {
		libraryImage.dispose();
		super.dispose();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent)  {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText("&Resolved entries:");
		viewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(new EntryContentProvider());
		viewer.setLabelProvider(new EntryLabelProvider());
		viewer.setSorter(new EntrySorter());
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				// Prevent user to change checkbox states
				viewer.setChecked(event.getElement(), !event.getChecked());
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		viewer.getTable().setLayoutData(gd);
		setControl(container);
		if (realEntries!=null) initializeView();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#finish
	 */
	public boolean finish()  {
		return true;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#getSelection
	 */
	public IClasspathEntry getSelection()  {
		return entry;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#setSelection
	 */
	public void setSelection(IClasspathEntry containerEntry)  {
		this.entry = containerEntry;
		createRealEntries();
		if (viewer!=null) initializeView();
	}
	
	private void createRealEntries() {
		IJavaProject javaProject = getJavaProject();
		if (javaProject!=null) {
			try {
				IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
				realEntries = container.getClasspathEntries();
			}
			catch (JavaModelException e) {
			}
		}
	}
	
	private IJavaProject getJavaProject() {
		if (entry==null) return null;
		IPath path = entry.getPath();
		if (path.segmentCount()<2) return null;
		String projectName = path.segment(1);
		IProject project = PDEPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project==null) return null;
		return JavaCore.create(project);
	}
	
	private void initializeView() {
		viewer.setInput(entry);
		viewer.setAllGrayed(true);
		ArrayList checked = new ArrayList();
		for (int i=0; i<realEntries.length; i++) {
			if (realEntries[i].isExported())
				viewer.setChecked(realEntries[i], true);
		}
	}
}
