/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionPointsSection extends TableSection {
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtensionPointSection.title";
	public static final String SECTION_DESC = "ManifestEditor.DetailExtensionPointSection.desc";
	public static final String SECTION_NEW = "ManifestEditor.DetailExtensionPointSection.new";
	private TableViewer pointTable;
	
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return new DummyExtensionPoint[] {
					new DummyExtensionPoint("point1", "Point 1", "schema/point1.exsd"),
					new DummyExtensionPoint("point2", "Point 2", "schema/point2.exsd") };					
		}
	}
	
	public ExtensionPointsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[]{PDEPlugin
				.getResourceString(SECTION_NEW)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		TablePart tablePart = getTablePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		pointTable = tablePart.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		//pointTable.setLabelProvider(new ExtensionPointLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		pointTable.setInput(getPage());		
	}
}