package org.eclipse.pde.internal.core.product;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;

public class ArgumentsInfo extends ProductObject implements IArgumentsInfo {

	private static final long serialVersionUID = 1L;
	private String fProgramArgs = ""; //$NON-NLS-1$
	private String fVMArgs = ""; //$NON-NLS-1$

	public ArgumentsInfo(IProductModel model) {
		super(model);
	}

	public void setProgramArguments(String args) {
		String old = fProgramArgs;
		fProgramArgs = args;
		if (isEditable())
			firePropertyChanged(P_PROG_ARGS, old, fProgramArgs);
	}

	public String getProgramArguments() {
		return fProgramArgs;
	}

	public void setVMArguments(String args) {
		String old = args;
		fVMArgs = args;
		if (isEditable())
			firePropertyChanged(P_VM_ARGS, old, fVMArgs);
	}

	public String getVMArguments() {
		return fVMArgs;
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(P_PROG_ARGS)) {
					fProgramArgs = getText(child);
				} else if (child.getNodeName().equals(P_VM_ARGS)) {
					fVMArgs = getText(child);
				}
			}
		}
	}
	
	private String getText(Node node) {
		node.normalize();
		Node text = node.getFirstChild();
		if (text != null && text.getNodeType() == Node.TEXT_NODE) {
			return text.getNodeValue();
		}
		return ""; //$NON-NLS-1$
	}
	
	public void write(String indent,java.io.PrintWriter writer) {
		writer.println(indent + "<launcherArgs>"); //$NON-NLS-1$
		if (fProgramArgs.length() > 0) {
			writer.println(indent + "   " + "<" + P_PROG_ARGS + ">" + getWritableString(fProgramArgs) + "</" + P_PROG_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (fVMArgs.length() > 0) {
			writer.println(indent + "   " + "<" + P_VM_ARGS + ">" + getWritableString(fVMArgs) + "</" + P_VM_ARGS + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		writer.println(indent + "</launcherArgs>"); //$NON-NLS-1$
	}
	
}
