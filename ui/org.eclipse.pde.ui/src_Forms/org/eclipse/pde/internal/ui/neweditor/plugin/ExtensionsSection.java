/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.TreePart;
import org.eclipse.swt.SWT;
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
		//extensionTree.setContentProvider(new ExtensionContentProvider());
		//extensionTree.setLabelProvider(new ExtensionLabelProvider());

		//drillDownAdapter = new DrillDownAdapter(extensionTree);
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}
}