package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TargetFeature extends TargetObject implements ITargetFeature {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fVersion;

	public TargetFeature(ITargetModel model) {
		super(model);
	}

	public String getId() {
		return fId.trim();
	}

	public void setId(String id) {
		fId = id;
	}

	public String getVersion() {
		return fVersion.trim();
	}

	public void setVersion(String version) {
		fVersion = version;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fVersion = element.getAttribute("version"); //$NON-NLS-1$
		}
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<feature id=\"" + getWritableString(fId) + "\" version=\"" + fVersion + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
