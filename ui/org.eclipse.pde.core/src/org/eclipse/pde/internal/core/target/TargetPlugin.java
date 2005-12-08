package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TargetPlugin extends TargetObject implements ITargetPlugin {

	private static final long serialVersionUID = 1L;
	private String fId;

	public TargetPlugin(ITargetModel model) {
		super(model);
	}

	public String getId() {
		return fId.trim();
	}

	public void setId(String id) {
		fId = id;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE)
			fId = ((Element)node).getAttribute("id"); //$NON-NLS-1$
	}

	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<plugin id=\"" + getWritableString(fId) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
