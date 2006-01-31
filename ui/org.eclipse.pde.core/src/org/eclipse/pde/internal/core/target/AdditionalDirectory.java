package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class AdditionalDirectory extends TargetObject implements
		IAdditionalLocation {

	private static final long serialVersionUID = 1L;
	
	private String fPath = ""; //$NON-NLS-1$
	
	public AdditionalDirectory(ITargetModel model) {
		super(model);
	}

	public String getPath() {
		return fPath;
	}

	public void setPath(String path) {
		String oldPath = fPath;
		fPath = path;
		firePropertyChanged(P_PATH, oldPath, fPath);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE)
			fPath = ((Element)node).getAttribute("path"); //$NON-NLS-1$
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<location path=\"" + getWritableString(fPath) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
