package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LocationInfo extends TargetObject implements ILocationInfo{

	private static final long serialVersionUID = 1L;
	
	private String fPath = ""; //$NON-NLS-1$

	public LocationInfo(ITargetModel model) {
		super(model);
	}

	public void parse(Node node) {
		Element element = (Element)node; 
		fPath = element.getAttribute("path");  //$NON-NLS-1$

	}

	public void write(String indent, PrintWriter writer) {
		if (fPath.length() == 0)
			return;
		writer.println();
		writer.println(indent + "<location path=\"" + getWritableString(fPath) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}



	public boolean useDefault() {
		return fPath.length() == 0;
	}



	public String getPath() {
		return fPath;
	}

	public void setPath(String path) {
		String oldValue = fPath;
		fPath = (path != null) ? path : ""; //$NON-NLS-1$
		firePropertyChanged(P_LOC, oldValue, fPath);
	}

}
