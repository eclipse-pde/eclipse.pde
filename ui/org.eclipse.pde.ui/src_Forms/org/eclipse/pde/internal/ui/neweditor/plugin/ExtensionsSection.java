/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.ArrayList;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionsSection extends TreeSection {
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtensionSection.title";
	public static final String SECTION_NEW = "ManifestEditor.DetailExtensionSection.new";
	public static final String SECTION_DOWN = "ManifestEditor.DetailExtensionSection.down";
	public static final String SECTION_UP = "ManifestEditor.DetailExtensionSection.up";
	
	private TreeViewer extensionTree;
	private ArrayList dummyContent;
	
	class ExtensionsContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof PDEFormPage)
				return dummyContent.toArray();
			if (parentElement instanceof DummyExtensionElement)
				return ((DummyExtensionElement)parentElement).getChildren();
			return new Object[0];
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof DummyExtensionElement)
				return ((DummyExtensionElement)element).getParent();
			return new Object[0];
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof DummyExtensionElement)
				return ((DummyExtensionElement)element).hasChildren();
			return false;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}
}

	public ExtensionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, 0, new String[]{
				PDEPlugin.getResourceString(SECTION_NEW), null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		handleDefaultButton = false;
	}
	
	public void createClient(
			Section section,
			FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);

		TreePart treePart = getTreePart();

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		extensionTree = treePart.getTreeViewer();
		extensionTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		extensionTree.setContentProvider(new ExtensionsContentProvider());
		//extensionTree.setLabelProvider(new ExtensionLabelProvider());

		//drillDownAdapter = new DrillDownAdapter(extensionTree);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		createDummyContent();
		extensionTree.setInput(getPage());
	}

	void collapseAll() {
		BusyIndicator.showWhile(extensionTree.getControl().getDisplay(),
				new Runnable() {
			public void run() {
				extensionTree.collapseAll();
			}
		});
	}
	
	private void createDummyContent() {
		dummyContent = new ArrayList();
		DummyExtension ex1 = new DummyExtension(null, null, "org.eclipse.ui.editors");
		DummyExtensionElement ed = new DummyExtensionElement("editor", ex1);
		ed.setProperty("default", "true");
		ed.setProperty("name", "%baseEditor");
		ed.setProperty("icon", "icons/file_obj.gif");
		ed.setProperty("extensions", "sef");
		ed.setProperty("class", "org.eclipse.ui.forms.examples.internal.rcp.SimpleFormEditor");
		ed.setProperty("id", "org.eclipse.ui.forms.examples.base-editor");
		dummyContent.add(ex1);
		
		DummyExtension ex2 = new DummyExtension(null, null, "org.eclipse.ui.actionSets");
		DummyExtensionElement set = new DummyExtensionElement("actionSet", ex2);
		set.setProperty("label", "Eclipse Forms Examples");
		set.setProperty("id", "org.eclipse.ui.forms.examples.actionSet");
		DummyExtensionElement menu = new DummyExtensionElement("menu", set);
		menu.setProperty("label", "Form Editors");
		menu.setProperty("id", "org.eclipse.ui.forms.examples.menu");
		DummyExtensionElement group = new DummyExtensionElement("groupMarker", menu);
		group.setProperty("name", "group");
		DummyExtensionElement action = new DummyExtensionElement("action", set);
		action.setProperty("label", "Simple Form Editor");
		action.setProperty("class", "org.eclipse.ui.forms.examples.internal.rcp.OpenSimpleFormEditorAction");
		action.setProperty("menubarPath", "org.eclipse.ui.forms.examples.menu/group");
		action.setProperty("id", "org.eclipse.ui.forms.examples.simple");
		dummyContent.add(ex2);		
	}
}