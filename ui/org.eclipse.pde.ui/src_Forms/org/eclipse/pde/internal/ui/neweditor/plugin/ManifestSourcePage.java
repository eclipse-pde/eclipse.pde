package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.util.ArrayList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.model.IDocumentNode;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.graphics.Image;

/**
 * @author melhem
 *
 */
public class ManifestSourcePage extends XMLSourcePage {
	
	private Object fLibraries = new Object();
	private Object fImports = new Object();
	private Object fExtensionPoints = new Object();
	private Object fExtensions = new Object();
	
	class OutlineLabelProvider extends LabelProvider {		
		private PDELabelProvider fProvider;
		
		public OutlineLabelProvider() {
			fProvider = PDEPlugin.getDefault().getLabelProvider();
			fProvider.connect(this);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		public void dispose() {
			fProvider.disconnect(this);
		}
		
		public String getText(Object obj) {
			//TODO translate strings
			if (obj == fLibraries)
				return "Libraries";
			if (obj == fImports)
				return "Dependencies";
			if (obj == fExtensionPoints)
				return "Extension Points";
			if (obj == fExtensions)
				return "Extensions";
			return fProvider.getText(obj);
		}

		public Image getImage(Object obj) {
			if (obj == fLibraries)
				return fProvider.get(PDEPluginImages.DESC_RUNTIME_OBJ);
			if (obj == fImports)
				return fProvider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
			if (obj == fExtensionPoints)
				return fProvider.get(PDEPluginImages.DESC_EXT_POINTS_OBJ);
			if (obj == fExtensions)
				return fProvider.get(PDEPluginImages.DESC_EXTENSIONS_OBJ);
			
			Image image = fProvider.getImage(obj);
			int flags = ((IDocumentNode)obj).isErrorNode() ? PDELabelProvider.F_ERROR : 0;
			return (flags == 0) ? image : fProvider.get(image, flags);
		}
	}
	
	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parent) {
			PluginModelBase model = (PluginModelBase)getInputContext().getModel();

			ArrayList result = new ArrayList();			
			if (parent instanceof IPluginBase) {
				IPluginBase pluginBase = (IPluginBase)parent;
				if (pluginBase.getLibraries().length > 0)
					result.add(fLibraries);
				if (pluginBase.getImports().length > 0)
					result.add(fImports);
				if (pluginBase.getExtensionPoints().length > 0)
					result.add(fExtensionPoints);
				if (pluginBase.getExtensions().length > 0)
					result.add(fExtensions);
				return result.toArray();
			} 
			if (parent == fLibraries)
				return model.getPluginBase().getLibraries();
			
			if (parent == fImports)
				return model.getPluginBase().getImports();
			
			if (parent == fExtensionPoints)
				return model.getPluginBase().getExtensionPoints();
			
			if (parent == fExtensions)
				return model.getPluginBase().getExtensions();
			
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IDocumentNode)
				return ((IDocumentNode)element).getParentNode();
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IPluginBase) {
				return ((IDocumentNode) element).getChildNodes().length > 0;
			}
			return element == fLibraries || element == fImports
			|| element == fExtensionPoints || element == fExtensions;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IPluginModelBase) {
				return new Object[] {((IPluginModelBase)inputElement).getPluginBase()};
			}
			return new Object[0];
		}
	}
		
	public ManifestSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.XMLSourcePage#dispose()
	 */
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	protected ILabelProvider createOutlineLabelProvider() {
		return new OutlineLabelProvider();
	}
	protected ITreeContentProvider createOutlineContentProvider() {
		return new ContentProvider();
	}

	protected void outlineSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentNode && !(first instanceof IPluginBase)) {
				setHighlightRange((IDocumentNode)first);				
			} else {
				resetHighlightRange();
			}
		}
	}
	
	public void setHighlightRange(IDocumentNode node) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = node.getOffset();
		int length = node.getLength();
		setHighlightRange(offset, length == -1 ? 1 : length, true);
		sourceViewer.setSelectedRange(offset + 1, node.getXMLTagName().length());
	}
	

}
