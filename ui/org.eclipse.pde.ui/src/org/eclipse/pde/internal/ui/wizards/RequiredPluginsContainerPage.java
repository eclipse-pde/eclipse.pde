package org.eclipse.pde.internal.ui.wizards;

import java.util.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.wizards.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * Insert the type's description here.
 * @see WizardPage
 */
public class RequiredPluginsContainerPage
	extends WizardPage
	implements IClasspathContainerPage, IClasspathContainerPageExtension {
	private IClasspathEntry entry;
	private CheckboxTableViewer viewer;
	private Image projectImage;
	private Image libraryImage;
	private IClasspathEntry[] realEntries;
	private IJavaProject javaProject;
	private Button attachSourceButton;
	private Hashtable replacedEntries;

	class EntryContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (realEntries != null)
				return realEntries;
			return new Object[0];
		}
	}

	class EntrySorter extends ViewerSorter {
		public int category(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			return entry.getEntryKind() == IClasspathEntry.CPE_PROJECT
				? -10
				: 0;
		}
	}

	class EntryLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return entry.getPath().segment(0);
			else
				return entry.getPath().toOSString();
		}

		public Image getImage(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return projectImage;
			else if (kind == IClasspathEntry.CPE_LIBRARY)
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
		projectImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_PROJECT);
		libraryImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
		setImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		replacedEntries = new Hashtable();
	}

	public void dispose() {
		libraryImage.dispose();
		super.dispose();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText("&Resolved entries:");
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		viewer =
			CheckboxTableViewer.newCheckList(
				container,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(new EntryContentProvider());
		viewer.setLabelProvider(new EntryLabelProvider());
		viewer.setSorter(new EntrySorter());
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				// Prevent user to change checkbox states
				viewer.setChecked(event.getElement(), !event.getChecked());
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged((IStructuredSelection) e.getSelection());
			}
		});
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		viewer.getTable().setLayoutData(gd);
		attachSourceButton = new Button(container, SWT.PUSH);
		attachSourceButton.setText("Attach &Source...");
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		attachSourceButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(attachSourceButton);
		attachSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAttachSource();
			}
		});
		attachSourceButton.setEnabled(false);
		setControl(container);
		if (realEntries != null)
			initializeView();
	}

	private void handleSelectionChanged(IStructuredSelection selection) {
		IClasspathEntry entry = (IClasspathEntry) selection.getFirstElement();
		boolean canAttach = true;
		if (entry == null
			|| entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY)
			canAttach = false;
		attachSourceButton.setEnabled(canAttach);
	}

	private IClasspathEntry getEditableEntry(IClasspathEntry entry) {
		IClasspathEntry modifiedEntry =
			(IClasspathEntry) replacedEntries.get(entry);
		if (modifiedEntry != null)
			return modifiedEntry;
		return entry;
	}

	private void handleAttachSource() {
		IStructuredSelection ssel =
			(IStructuredSelection) viewer.getSelection();
		IClasspathEntry entry = (IClasspathEntry) ssel.getFirstElement();
		IClasspathEntry editableEntry = getEditableEntry(entry);

		SourceAttachmentDialog dialog =
			new SourceAttachmentDialog(
				viewer.getControl().getShell(),
				editableEntry);
		IClasspathEntry newEntry = null;
		if (dialog.open() == SourceAttachmentDialog.OK) {
			newEntry = dialog.getNewEntry();
			replacedEntries.put(entry, newEntry);
		}
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#finish
	 */
	public boolean finish() {
		if (replacedEntries.size()>0) {
			// must handle edited entries
			processReplacedEntries();
		}
		return true;
	}

	private void processReplacedEntries() {
		SourceAttachmentManager manager = PDECore.getDefault().getSourceAttachmentManager();
		for (Enumeration enum=replacedEntries.keys(); enum.hasMoreElements();) {
			IClasspathEntry entry = (IClasspathEntry)enum.nextElement();
			IClasspathEntry newEntry = (IClasspathEntry)replacedEntries.get(entry);
			manager.addEntry(newEntry.getPath(), newEntry.getSourceAttachmentPath(), newEntry.getSourceAttachmentRootPath());
		}
		manager.save();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#getSelection
	 */
	public IClasspathEntry getSelection() {
		return entry;
	}

	public void initialize(
		IJavaProject project,
		IClasspathEntry[] currentEntries) {
		javaProject = project;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#setSelection
	 */
	public void setSelection(IClasspathEntry containerEntry) {
		this.entry = containerEntry;
		createRealEntries();
		if (viewer != null)
			initializeView();
	}

	private void createRealEntries() {
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			try {
				IClasspathContainer container =
					JavaCore.getClasspathContainer(
						entry.getPath(),
						javaProject);
				realEntries = container.getClasspathEntries();
			} catch (JavaModelException e) {
			}
		}
	}

	private IJavaProject getJavaProject() {
		return javaProject;
	}

	private void initializeView() {
		viewer.setInput(entry);
		viewer.setAllGrayed(true);
		for (int i = 0; i < realEntries.length; i++) {
			if (realEntries[i].isExported())
				viewer.setChecked(realEntries[i], true);
		}
	}
}
