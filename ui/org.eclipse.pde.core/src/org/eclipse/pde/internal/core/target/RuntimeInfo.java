package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.IRuntimeInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RuntimeInfo extends TargetObject implements IRuntimeInfo {

	private static final long serialVersionUID = 1L;
	private int fType;
	private String fName;

	public RuntimeInfo(ITargetModel model) {
		super(model);
	}

	public int getJREType() {
		return fType;
	}

	public String getJREName() {
		return fName;
	}

	public void setNamedJRE(String name) {
		int oldType = fType;
		String oldName = fName;
		fName =  (name == null) ? "" : name; //$NON-NLS-1$
		fType = TYPE_NAMED;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
		else
			firePropertyChanged(P_TARGET_JRE, oldName, fName);
	}

	public void setExecutionEnvJRE(String name) {
		int oldType = fType;
		String oldName = fName;
		fName =  (name == null) ? "" : name; //$NON-NLS-1$
		fType = TYPE_EXECUTION_ENV;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
		else
			firePropertyChanged(P_TARGET_JRE, oldName, fName);
	}

	public void setDefaultJRE() {
		int oldType = fType;
		fName =  "";  //$NON-NLS-1$
		fType = TYPE_DEFAULT;
		if (oldType != fType)
			firePropertyChanged(P_TARGET_JRE, new Integer(oldType), new Integer(fType));
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("jreName")) { //$NON-NLS-1$
					fType = TYPE_NAMED;
					fName = getText(child);
				} else if (child.getNodeName().equals("execEnv")) { //$NON-NLS-1$
					fType = TYPE_EXECUTION_ENV;
					fName = getText(child);
				} 
			}
		}
		if (list.getLength() == 0) {
			fType = TYPE_DEFAULT;
			fName = ""; //$NON-NLS-1$
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

	public void write(String indent, PrintWriter writer) {
		if (fType == 0) 
			return;
		writer.println();
		writer.println(indent + "<targetJRE>"); //$NON-NLS-1$
		if (fType == 1) 
			writer.println(indent + "   <jreName>" + getWritableString(fName) + "</jreName>"); //$NON-NLS-1$ //$NON-NLS-2$
		else if (fType == 2)
			writer.println(indent + "   <execEnv>" + getWritableString(fName) + "</execEnv>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(indent + "</targetJRE>"); //$NON-NLS-1$
	}

}
