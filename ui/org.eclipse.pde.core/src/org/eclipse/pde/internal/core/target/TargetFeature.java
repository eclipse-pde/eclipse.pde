package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TargetFeature extends TargetObject implements ITargetFeature {

	private static final long serialVersionUID = 1L;
	private String fId;
	private boolean fOptional = false;

	public TargetFeature(ITargetModel model) {
		super(model);
	}

	public String getId() {
		return fId.trim();
	}

	public void setId(String id) {
		fId = id;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fOptional = element.getAttribute("optional").equalsIgnoreCase("true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void write(String indent, PrintWriter writer) {
		//TODO add support for optional
		writer.print(indent + "<feature id=\"" + getWritableString(fId)); //$NON-NLS-1$ 
		writer.println((fOptional) ? "\" optional=\"true\"/>" : "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isOptional() {
		return fOptional;
	}

	public void setOptional(boolean optional) {
		fOptional = optional;
	}

}
