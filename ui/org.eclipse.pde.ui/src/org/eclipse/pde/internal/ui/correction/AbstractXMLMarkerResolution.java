package org.eclipse.pde.internal.ui.correction;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.core.text.plugin.PluginObjectNode;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;

public abstract class AbstractXMLMarkerResolution extends AbstractPDEMarkerResolution {

	protected String fLocationPath;
	private boolean fFragmentChange;
	
	public AbstractXMLMarkerResolution(int resolutionType, IMarker marker) {
		super(resolutionType);
		fFragmentChange = marker.getResource().getName().equals("fragment.xml"); //$NON-NLS-1$
		try {
			fLocationPath = (String)marker.getAttribute(PDEMarkerFactory.PK_TREE_LOCATION_PATH);
		} catch (CoreException e) {
		}
	}
	
	protected AbstractEditingModel createModel(IDocument doc) {
		if (fFragmentChange)
			return new FragmentModel(doc, true);
		return new PluginModel(doc, true);
	}
	
	protected abstract void createChange(PluginModelBase model);
	
	protected void createChange(AbstractEditingModel model) {
		if (model instanceof PluginModelBase)
			createChange((PluginModelBase)model);
	}
	
	protected IModelTextChangeListener createListener(IDocument doc) {
		return new XMLTextChangeListener(doc);
	}
	
	protected PluginObjectNode findNode(PluginModelBase base, String nodePath) {
		if (nodePath == null)
			return null;
		
		PluginObjectNode node = (PluginObjectNode)base.getPluginBase();
		StringTokenizer strtok = new StringTokenizer(nodePath, ">"); //$NON-NLS-1$
		while (strtok.hasMoreTokens()) {
			String token = strtok.nextToken();
			int childIndex = Integer.parseInt(token.substring(1, token.indexOf(')')));
			token = token.substring(token.indexOf(')') + 1);
			IDocumentNode[] children = node.getChildNodes();
			node = (PluginObjectNode)children[childIndex];
		}
		return node;
	}
	
}
