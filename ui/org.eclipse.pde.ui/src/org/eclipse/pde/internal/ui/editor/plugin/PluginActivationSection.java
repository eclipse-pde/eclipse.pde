/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.PageBook;
import org.osgi.framework.*;

public class PluginActivationSection extends TableSection
		implements
			IModelChangedListener, IInputContextListener {
	private PageBook topBook;
	private Composite topContainer;
	private Composite blankContainer;
	private TableViewer fExceptionsTableViewer;
	private Button fDoActivateButton;
	private Button fDoNotActivateButton;
	private Font fBoldFont;
	private static final String ECLIPSE_AUTOSTART = PDEPlugin.getResourceString("PluginActivationSection.autostart"); //$NON-NLS-1$

	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getExceptions();
		}
	}
	class TableLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}
	public PluginActivationSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[]{
						PDEPlugin
								.getResourceString("ManifestEditor.OSGiSection.add"), //$NON-NLS-1$
						PDEPlugin
								.getResourceString("ManifestEditor.OSGiSection.remove")}); //$NON-NLS-1$
		getSection().setText(PDEPlugin.getResourceString("PluginActivationSection.title")); //$NON-NLS-1$
		getSection()
				.setDescription(
						PDEPlugin.getResourceString("PluginActivationSection.desc")); //$NON-NLS-1$
	}
	private void update() {
		fDoActivateButton.setEnabled(isEditable());
		fDoActivateButton.setSelection(isAutoStart());
		fDoNotActivateButton.setSelection(!isAutoStart());
		fDoNotActivateButton.setEnabled(isEditable());
		enableButtons();
	}

	private boolean isAutoStart() {
		ManifestElement element = getManifestElement();
		return (element == null) ? true : !"false".equals(element.getValue()); //$NON-NLS-1$
	}

	private String[] getExceptions() {
		ManifestElement element = getManifestElement();
		if (element == null)
			return new String[0];

		String exceptions = element.getAttribute("exceptions"); //$NON-NLS-1$
		if (exceptions == null)
			return new String[0];

		ArrayList tokens = new ArrayList();
		StringTokenizer tok = new StringTokenizer(exceptions, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens())
			tokens.add(tok.nextToken().trim());
		return (String[]) tokens.toArray(new String[tokens.size()]);
	}

	private ManifestElement getManifestElement() {
		IBundleModel model = getBundleModel();
		if (model != null) {
			String value = model.getBundle().getHeader(ECLIPSE_AUTOSTART);
			if (value != null) {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(
							ECLIPSE_AUTOSTART, value);
					if (elements != null && elements.length > 0)
						return elements[0];
				} catch (BundleException e) {
				}
			}
		}
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#isEditable()
	 */
	public boolean isEditable() {
		return getPage().getModel().isEditable()
				&& getBundleModel() != null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		InputContextManager contextManager = getPage().getPDEEditor().getContextManager();
		if (contextManager!=null)
			contextManager.removeInputContextListener(this);		
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
		fBoldFont.dispose();
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (event.getChangedProperty().equals(ECLIPSE_AUTOSTART)) {
			refresh();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fExceptionsTableViewer.refresh();
		fDoActivateButton.setSelection(isAutoStart());
		fDoNotActivateButton.setSelection(!isAutoStart());
		super.refresh();
	}

	private void initializeFonts() {
		FontData[] fontData = getSection().getFont().getFontData();
		FontData data;
		if (fontData.length > 0)
			data = fontData[0];
		else
			data = new FontData();
		data.setStyle(SWT.BOLD);
		fBoldFont = new Font(getSection().getDisplay(), fontData);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		initializeFonts();

		Composite mainContainer = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.verticalSpacing = 5;
		mainContainer.setLayout(layout);
		mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		/*
		 * create new manifest part
		 */
		if (getPage().getPDEEditor().getAggregateModel().isEditable()) {
			topBook = new PageBook(mainContainer, SWT.NULL);
			//create a blank container that will be used
			// to hide the text and the link when not needed
			blankContainer = toolkit.createComposite(topBook);
			blankContainer.setLayout(new GridLayout());
			Label label = toolkit.createLabel(blankContainer, null);
			GridData gd = new GridData();
			gd.heightHint = 1;
			gd.widthHint = 1;
			label.setLayoutData(gd);

			topContainer = toolkit.createComposite(topBook);
			layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 2;
			layout.numColumns = 2;
			layout.makeColumnsEqualWidth = false;
			topContainer.setLayout(layout);
			toolkit
					.createLabel(
							topContainer,
							PDEPlugin.getResourceString("PluginActivationSection.manifestRequired")); //$NON-NLS-1$
			Hyperlink manifestLink = toolkit.createHyperlink(topContainer,
					PDEPlugin.getResourceString("PluginActivationSection.createManifest"), SWT.NULL); //$NON-NLS-1$
			manifestLink.addHyperlinkListener(new IHyperlinkListener() {
				public void linkActivated(HyperlinkEvent e) {
					handleConvert();
				}
				public void linkExited(HyperlinkEvent e) {
				}
				public void linkEntered(HyperlinkEvent e) {
				}
			});
			bundleModeChanged(getContextId().equals(BundleInputContext.CONTEXT_ID));
		}

		/*
		 * Bottom parts (Activation Rule & Exceptions)
		 */
		Composite bottomContainer = toolkit.createComposite(mainContainer);
		layout = new GridLayout();
		layout.makeColumnsEqualWidth = true;
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 2;
		bottomContainer.setLayout(layout);
		bottomContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		/*
		 * Activation rule part
		 */
		Composite ruleContainer = toolkit.createComposite(bottomContainer);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		ruleContainer.setLayout(layout);
		ruleContainer.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING));

		Label activateLabel = toolkit.createLabel(ruleContainer,
				PDEPlugin.getResourceString("PluginActivationSection.rule")); //$NON-NLS-1$
		activateLabel.setFont(fBoldFont);
		
		toolkit.createLabel(ruleContainer, PDEPlugin.getResourceString("PluginActivationSection.ruleLabel")); //$NON-NLS-1$

		fDoActivateButton = toolkit.createButton(ruleContainer,
				PDEPlugin.getResourceString("PluginActivationSection.activate"), SWT.RADIO); //$NON-NLS-1$

		GridData gd = new GridData();
		gd.horizontalIndent = 5;
		fDoActivateButton.setLayoutData(gd);
		fDoActivateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				writeHeader();
			}
		});
		/*
		 * auto-activate should be set to true by default with empty exceptions
		 * package list
		 */
		fDoNotActivateButton = toolkit.createButton(ruleContainer,
				PDEPlugin.getResourceString("PluginActivationSection.noActivate"), SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 5;
		fDoNotActivateButton.setLayoutData(gd);
		fDoNotActivateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				writeHeader();
			}
		});
		/*
		 * Exceptions part
		 */
		Composite exceptionsContainer = toolkit
				.createComposite(bottomContainer);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.verticalSpacing = 3;
		exceptionsContainer.setLayout(layout);
		exceptionsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label exceptionLabel = toolkit.createLabel(exceptionsContainer,
				PDEPlugin.getResourceString("PluginActivationSection.exception.title")); //$NON-NLS-1$
		exceptionLabel.setFont(fBoldFont);
		exceptionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label label = toolkit
				.createLabel(
						exceptionsContainer,
						PDEPlugin.getResourceString("PluginActivationSection.exception.desc"), //$NON-NLS-1$
						SWT.WRAP);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 225;
		label.setLayoutData(gd);

		Composite exceptionsPkgContainer = toolkit
				.createComposite(exceptionsContainer);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		exceptionsPkgContainer.setLayout(layout);
		exceptionsPkgContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		createViewerPartControl(exceptionsPkgContainer, SWT.FULL_SELECTION, 2,
				toolkit);
		fExceptionsTableViewer = getTablePart().getTableViewer();
		fExceptionsTableViewer.setContentProvider(new TableContentProvider());
		fExceptionsTableViewer.setLabelProvider(new TableLabelProvider());
		fExceptionsTableViewer.setInput(getBundleModel());

		toolkit.paintBordersFor(exceptionsContainer);
		section.setClient(mainContainer);
		IBundleModel model = getBundleModel();
		if (model != null)
			model.addModelChangedListener(this);
		InputContextManager contextManager = getPage().getPDEEditor().getContextManager();	
		if (contextManager!=null)
			contextManager.addInputContextListener(this);
		update();
	}
	
	private void handleConvert() {
		try {
			getPage().getEditor().doSave(null);
			IPluginModelBase model = (IPluginModelBase) getPage()
					.getPDEEditor().getAggregateModel();
			IResource resource = model.getUnderlyingResource();
			
			TreeSet list = new TreeSet();
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				addLibraryPackageFragments(resource.getProject(), libraries[i], list);
			}
			PDEPluginConverter.convertToOSGIFormat(resource.getProject(), resource.getName(), (String[]) list.toArray(new String[list.size()]), new NullProgressMonitor()); //$NON-NLS-1$

			InputContextManager mgr = getPage().getPDEEditor().getContextManager();
			InputContext context = mgr.findContext(PluginInputContext.CONTEXT_ID);
			IDocument doc = context.getDocumentProvider().getDocument(context.getInput());
			FindReplaceDocumentAdapter adapter = new FindReplaceDocumentAdapter(doc);
			MultiTextEdit multiEdit = new MultiTextEdit();
			TextEdit edit = editRootElement(model.isFragmentModel() ? "fragment" : "plugin", adapter, doc, 0); //$NON-NLS-1$ //$NON-NLS-2$
			if (edit != null)
				multiEdit.addChild(edit);
			edit = removeElement("requires", adapter, doc, 0); //$NON-NLS-1$
			if (edit != null)
				multiEdit.addChild(edit);
			edit = removeElement("runtime", adapter, doc, 0); //$NON-NLS-1$
			if (edit != null)
				multiEdit.addChild(edit);
			
			if (multiEdit.hasChildren()) {
				multiEdit.apply(doc);
				getPage().getEditor().doSave(null);
			}
		} catch (Exception e1) {
		}
		
	}
	
	private void addLibraryPackageFragments(IProject project,
			IPluginLibrary library, Set set) {
		try {
			if (!project.hasNature(JavaCore.NATURE_ID))
				return;
			
			if (library.isFullyExported()) {
				IPackageFragmentRoot[] roots = getPackageFragmentRoots(
						project, ClasspathUtilCore.expandLibraryName(library.getName()));
				for (int i = 0; i < roots.length; i++) {
					IJavaElement[] fragments = roots[i].getChildren();
					for (int j = 0; j < fragments.length; j++) {
						if (fragments[j] instanceof IPackageFragment) {
							if (((IPackageFragment)fragments[j]).hasChildren())
								set.add(fragments[j].getElementName());
						}
					}
				}
			}

			String[] filters = library.getContentFilters();
			for (int i = 0; i < filters.length; i++) {
				String filter = filters[i].trim();
				if (filter.endsWith(".*")) { //$NON-NLS-1$
					set.add(filter.substring(0, filter.length() - 2));
				} else {
					set.add(filter);
				}
			}
		} catch (CoreException e) {
			return;
		}
	}
	
	private IPackageFragmentRoot[] getPackageFragmentRoots(IProject project, String libraryName) {
		IJavaProject jProject = JavaCore.create(project);
		IResource resource = project.findMember(libraryName);
		if (resource != null) {
			IPackageFragmentRoot root = jProject.getPackageFragmentRoot(libraryName);
			if (root.exists())
				return new IPackageFragmentRoot[] {root};
		}
			
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context != null) {
			IBuildModel model = (IBuildModel)context.getModel();
			IBuildEntry entry = model.getBuild().getEntry("source." + libraryName); //$NON-NLS-1$
			if (entry != null) {
				String[] tokens = entry.getTokens();
				ArrayList list = new ArrayList();
				for (int i = 0; i < tokens.length; i++) {
					resource = project.findMember(tokens[i]);
					if (resource != null) {
						IPackageFragmentRoot root = jProject.getPackageFragmentRoot(resource);
						if (root.exists())
							list.add(root);
					}
				}
				return (IPackageFragmentRoot[])list.toArray(new IPackageFragmentRoot[list.size()]);
			}
		}
		return new IPackageFragmentRoot[0];
	}
	
	private TextEdit editRootElement(String elementName, FindReplaceDocumentAdapter adapter, IDocument doc, int offset) {
		try {
			IRegion region = adapter.find(0, "<" + elementName + "[^>]*", true, true, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			if (region != null) {
				String replacementString = "<" + elementName; //$NON-NLS-1$
				if (doc.getChar(region.getOffset() + region.getLength()) == '/')
					replacementString += "/"; //$NON-NLS-1$
				return new ReplaceEdit(region.getOffset(), region.getLength(), replacementString);
			}
		} catch (BadLocationException e) {
		}
		return null;
	}
	private TextEdit removeElement(String elementName, FindReplaceDocumentAdapter adapter, IDocument doc, int offset) {
		try {
			IRegion region = adapter.find(0, "<" + elementName + "[^>]*", true, true, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			if (region != null) {
				if (doc.getChar(region.getOffset() + region.getLength()) == '/')
					return new DeleteEdit(region.getOffset(), region.getLength() + 1);
				IRegion endRegion = adapter.find(0, "</" + elementName +">", true, true, false, true); //$NON-NLS-1$ //$NON-NLS-2$
				if (endRegion != null) {
					int lastPos = endRegion.getOffset() + endRegion.getLength() + 1;
					while (Character.isWhitespace(doc.getChar(lastPos))) {
						lastPos += 1;
					}
					lastPos -= 1;
					return new DeleteEdit(region.getOffset(), lastPos - region.getOffset());
				}
			}
		} catch (BadLocationException e) {
		}
		return null;
	}
	
	protected void enableButtons() {
		getTablePart().setButtonEnabled(0, isEditable());
		getTablePart().setButtonEnabled(1, false);
	}
	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleRemove();
	}
	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				TableItem[] existingPackages = fExceptionsTableViewer
						.getTable().getItems();
				Vector existing = new Vector();
				for (int i = 0; i < existingPackages.length; i++) {
					existing.add(existingPackages[i].getText());
				}
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				PackageSelectionDialog dialog = new PackageSelectionDialog(
						fExceptionsTableViewer.getTable().getShell(),
						labelProvider, JavaCore.create(project), existing);
				if (dialog.open() == PackageSelectionDialog.OK) {
					Object[] elements = dialog.getResult();
					for (int i = 0; i < elements.length; i++) {
						fExceptionsTableViewer
								.add(((IPackageFragment) elements[i])
										.getElementName());
					}
					writeHeader();
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}

	private void writeHeader() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(fDoActivateButton.getSelection() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		TableItem[] items = fExceptionsTableViewer.getTable().getItems();
		if (items.length > 0)
			buffer.append(";exceptions=\""); //$NON-NLS-1$
		for (int i = 0; i < items.length; i++) {
			if (i > 0)
				buffer.append(" "); //$NON-NLS-1$
			buffer.append(items[i].getData().toString());
			if (i < items.length - 1)
				buffer.append("," + System.getProperty("line.separator")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (items.length > 0)
			buffer.append("\""); //$NON-NLS-1$
		getBundleModel().getBundle().setHeader(ECLIPSE_AUTOSTART,
				buffer.toString());
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fExceptionsTableViewer
				.getSelection();
		Object[] items = ssel.toArray();
		for (int i = 0; i < items.length; i++) {
			fExceptionsTableViewer.remove(items[i]);
		}
		writeHeader();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.TableSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		getTablePart().setButtonEnabled(1,
				selection != null && !selection.isEmpty());
	}

	public String getContextId() {
		if (getPluginBase() instanceof IBundlePluginBase)
			return BundleInputContext.CONTEXT_ID;
		return PluginInputContext.CONTEXT_ID;
	}
	private IPluginBase getPluginBase() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}

	private IBundleModel getBundleModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
				.findContext(BundleInputContext.CONTEXT_ID);
		return context != null ? (IBundleModel) context.getModel() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextAdded(InputContext context) {
		if (!context.getId().equals(BundleInputContext.CONTEXT_ID))
			return;
		// bundle added - remove the text for manifest creation
		// and enable controls
		bundleModeChanged(true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		if (!context.getId().equals(BundleInputContext.CONTEXT_ID))
			return;
		// bundle removed - add the text for manifest creation
		// and disable controls
		bundleModeChanged(false);
	}
	private void bundleModeChanged(boolean added) {
		if (added && getPage().getModel().isEditable()) {
			topBook.showPage(blankContainer);
		}
		else {
			topBook.showPage(topContainer);
		}
		if (fDoActivateButton!=null) {
			update();
			topBook.getParent().layout();
			getManagedForm().reflow(true);
			fExceptionsTableViewer.setInput(getBundleModel());
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return false;
	}
}
