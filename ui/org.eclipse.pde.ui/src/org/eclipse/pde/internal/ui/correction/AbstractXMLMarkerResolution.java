package org.eclipse.pde.internal.ui.correction;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginObjectNode;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;

public abstract class AbstractXMLMarkerResolution extends AbstractPDEMarkerResolution {

	protected String fLocationPath;
	private boolean fFragmentChange;
	
	public AbstractXMLMarkerResolution(int resolutionType, IMarker marker) {
		super(resolutionType);
		fFragmentChange = marker.getResource().getName().equals("fragment.xml"); //$NON-NLS-1$
		try {
			fLocationPath = (String)marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
	}
	
	protected AbstractEditingModel createModel(IDocument doc) {
		if (fFragmentChange)
			return new FragmentModel(doc, true);
		return new PluginModel(doc, true);
	}
	
	protected abstract void createChange(IPluginModelBase model);
	
	protected void createChange(IBaseModel model) {
		if (model instanceof IPluginModelBase)
			createChange((IPluginModelBase)model);
	}
	
	protected IModelTextChangeListener createListener(IDocument doc) {
		return new XMLTextChangeListener(doc);
	}
	
	protected Object findNode(IPluginModelBase base) {
		if (fLocationPath == null)
			return null;
		
		// special case for externalizing strings in manifest.mf
		if (fLocationPath.charAt(0) != '(' &&
				base instanceof IBundlePluginModelBase) {
			IBundle bundle = ((IBundlePluginModelBase)base).getBundleModel().getBundle();
			return bundle.getManifestHeader(fLocationPath);
		}
		
		IDocumentNode node = (PluginObjectNode)base.getPluginBase();
		StringTokenizer strtok = new StringTokenizer(fLocationPath, ">"); //$NON-NLS-1$
		while (node != null && strtok.hasMoreTokens()) {
			String token = strtok.nextToken();
			int childIndex = Integer.parseInt(token.substring(1, token.indexOf(')')));
			token = token.substring(token.indexOf(')') + 1);
			IDocumentNode[] children = node.getChildNodes();
			node = children[childIndex];
			int attr = token.indexOf(XMLErrorReporter.F_ATT_PREFIX); 
			if (attr != -1)
				return node.getDocumentAttribute(token.substring(attr + 1));
		}
		return node;
	}
	
	protected String getNameOfNode() {
		int lastChild = fLocationPath.lastIndexOf(')');
		if (lastChild < 0)
			return fLocationPath;
		String item = fLocationPath.substring(lastChild + 1);
		lastChild = item.indexOf(XMLErrorReporter.F_ATT_PREFIX); 
		if (lastChild == -1)
			return item;
		return item.substring(lastChild + 1);
	}
	
	protected boolean isAttrNode() {
		return fLocationPath.indexOf(XMLErrorReporter.F_ATT_PREFIX) != -1;
	}
}
