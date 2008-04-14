package org.eclipse.pde.internal.ds.ui.editor;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage.BasicLabelProvider;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSDefinitionPage;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSFormOutlinePage.SimpleCSLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;

public class DSFormOutlinePage extends FormOutlinePage  {

	public DSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	
	//TODO
}
