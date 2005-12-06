package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EnvironmnetInfo extends TargetObject implements IEnvironmentInfo {

	private static final long serialVersionUID = 1L;
	
	private String fOS;
	private String fWS;
	private String fArch;
	private String fNL;
	
	public EnvironmnetInfo(ITargetModel model) {
		super(model);
	}

	public String getOS() {
		return fOS;
	}

	public String getWS() {
		return fWS;
	}

	public String getArch() {
		return fArch;
	}

	public String getNL() {
		return fNL;
	}

	public void setOS(String os) {
		String oldValue = fOS;
		fOS = os;
		firePropertyChanged(P_OS, oldValue, fOS);
	}

	public void setWS(String ws) {
		String oldValue = fWS;
		fWS = ws;
		firePropertyChanged(P_WS, oldValue, fWS);
	}

	public void setArch(String arch) {
		String oldValue = fArch;
		fArch = arch;
		firePropertyChanged(P_ARCH, oldValue, fArch);
	}

	public void setNL(String nl) {
		String oldValue = fNL;
		fNL = nl;
		firePropertyChanged(P_NL, oldValue, fNL);
	}

	public void parse(Node node) {
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(P_OS)) {
					fOS = getText(child);
				} else if (child.getNodeName().equals(P_WS)) {
					fWS = getText(child);
				} else if (child.getNodeName().equals(P_ARCH)) {
					fArch = getText(child);
				} else if (child.getNodeName().equals(P_NL)) {
					fNL = getText(child);
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
	
	public void write(String indent, PrintWriter writer) {
		// if no elements set, then don't write anything
		if ((fOS == null || fOS.length() == 0) && (fWS == null || fWS.length() == 0) &&
				(fArch == null || fArch.length() == 0) && (fNL == null || fNL.length() == 0))
			return;
		writer.println();
		writer.println(indent + "<environment>"); //$NON-NLS-1$
		if (fOS != null && fOS.length() > 0)
			writer.println(indent + "   <" + P_OS + ">" + getWritableString(fOS) + "</" + P_OS + ">");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (fWS != null && fWS.length() > 0)
			writer.println(indent + "   <" + P_WS + ">" + getWritableString(fWS) + "</" + P_WS + ">");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (fArch != null && fArch.length() > 0)
			writer.println(indent + "   <" + P_ARCH + ">" + getWritableString(fArch) + "</" + P_ARCH + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (fNL != null && fNL.length() > 0)
			writer.println(indent + "   <" + P_NL + ">" + getWritableString(fNL) + "</" + P_NL + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		writer.println(indent + "</environment>"); //$NON-NLS-1$
	}

}
