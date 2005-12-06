package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArgumentsInfo extends TargetObject implements IArgumentsInfo {

	private static final long serialVersionUID = 1L;
	private String fProgramArgs = ""; //$NON-NLS-1$
	private String fVMArgs = ""; //$NON-NLS-1$
	
	public ArgumentsInfo(ITargetModel model) {
		super(model);
	}
	
	public String getProgramArguments() {
		return fProgramArgs;
	}
	
	public String getVMArguments() {
		return fVMArgs;
	}
	
	public void setProgramArguments(String args) {
		String oldValue = fProgramArgs;
		fProgramArgs = args; 
		if (isEditable())
			firePropertyChanged(P_PROG_ARGS, oldValue, fProgramArgs);
	}
	
	public void setVMArguments(String args) {
		String oldValue = fVMArgs;
		fVMArgs = args;
		if (isEditable())
			firePropertyChanged(P_VM_ARGS, oldValue, fVMArgs);
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
		if (( fProgramArgs == null || fProgramArgs.length()== 0) && (fVMArgs == null || fVMArgs.length() == 0))
			return;
		writer.println();
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
