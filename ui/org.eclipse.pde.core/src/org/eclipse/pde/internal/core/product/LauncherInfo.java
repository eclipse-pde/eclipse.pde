package org.eclipse.pde.internal.core.product;

import java.io.*;
import java.util.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;

public class LauncherInfo extends ProductObject implements ILauncherInfo {

	private static final long serialVersionUID = 1L;
	private boolean fUseIcoFile;
	private String fIcoFilePath;
	private Map fIcons = new HashMap();
	private String fLauncherName;

	public LauncherInfo(IProductModel model) {
		super(model);
	}

	public String getLauncherName() {
		return fLauncherName;
	}

	public void setLauncherName(String name) {
		String old = fLauncherName;
		fLauncherName = name;
		if (isEditable())
			firePropertyChanged(P_LAUNCHER, old, fLauncherName);
	}

	public void setIconPath(String iconId, String path) {
		if (path == null)
			path = ""; //$NON-NLS-1$
		String old = (String)fIcons.get(iconId);
		fIcons.put(iconId, path);
		if (isEditable())
			firePropertyChanged(iconId, old, path);
	}

	public String getIconPath(String iconId) {
		return (String)fIcons.get(iconId);
	}

	public boolean usesWinIcoFile() {
		return fUseIcoFile;
	}

	public void setUseWinIcoFile(boolean use) {
		boolean old = fUseIcoFile;
		fUseIcoFile = use;
		if (isEditable())
			firePropertyChanged(P_USE_ICO, Boolean.toString(old), Boolean.toString(fUseIcoFile));
	}

	public void setIcoFilePath(String path) {
		String old = fIcoFilePath;
		fIcoFilePath = path;
		if (isEditable())
			firePropertyChanged(P_ICO_PATH, old, fIcoFilePath);
	}

	public String getIcoFilePath() {
		return fIcoFilePath;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			fLauncherName = ((Element)node).getAttribute("name"); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("linux")) { //$NON-NLS-1$
						parseLinux((Element)child);
					} else if (name.equals("macosx")) { //$NON-NLS-1$
						parseMac((Element)child);
					} else if (name.equals("solaris")) { //$NON-NLS-1$
						parseSolaris((Element)child);
					} else if (name.equals("win")) { //$NON-NLS-1$
						parseWin((Element)child);
					}
				}
			}
		}
	}
	
	private void parseWin(Element element) {
		fUseIcoFile = "true".equals(element.getAttribute(P_USE_ICO)); //$NON-NLS-1$
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)children.item(i);
				String name = child.getNodeName();
				if (name.equals("ico")) { //$NON-NLS-1$
					fIcoFilePath = child.getAttribute("path"); //$NON-NLS-1$
				} else if (name.equals("bmp")) { //$NON-NLS-1$
					fIcons.put(WIN32_16_HIGH, element.getAttribute(WIN32_16_HIGH));
					fIcons.put(WIN32_16_LOW, element.getAttribute(WIN32_16_LOW));
					fIcons.put(WIN32_32_HIGH, element.getAttribute(WIN32_32_HIGH));
					fIcons.put(WIN32_32_LOW, element.getAttribute(WIN32_32_LOW));
					fIcons.put(WIN32_48_HIGH, element.getAttribute(WIN32_48_HIGH));
					fIcons.put(WIN32_48_LOW, element.getAttribute(WIN32_48_LOW));
				}
			}
		}
	}

	private void parseSolaris(Element element) {
		fIcons.put(SOLARIS_LARGE, element.getAttribute(SOLARIS_LARGE));
		fIcons.put(SOLARIS_MEDIUM, element.getAttribute(SOLARIS_MEDIUM));
		fIcons.put(SOLARIS_SMALL, element.getAttribute(SOLARIS_SMALL));
		fIcons.put(SOLARIS_TINY, element.getAttribute(SOLARIS_TINY));
	}

	private void parseMac(Element element) {
		fIcons.put(MACOSX_ICON, element.getAttribute(MACOSX_ICON));
	}

	private void parseLinux(Element element) {
		fIcons.put(LINUX_ICON, element.getAttribute(LINUX_ICON));
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<launcher "); //$NON-NLS-1$
		if (fLauncherName != null && fLauncherName.length() > 0)
			writer.print("name=\"" + fLauncherName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(">"); //$NON-NLS-1$
		
		writeLinux(indent + "   ", writer); //$NON-NLS-1$
		writeMac(indent + "   ", writer); //$NON-NLS-1$
		writeSolaris(indent + "   ", writer); //$NON-NLS-1$
		writerWin(indent + "   ", writer); //$NON-NLS-1$
		writer.println(indent + "</launcher>"); //$NON-NLS-1$
	}

	private void writerWin(String indent, PrintWriter writer) {
		writer.println(indent + "<win " + P_USE_ICO + "=\"" + Boolean.toString(fUseIcoFile) + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (fIcoFilePath != null && fIcoFilePath.length() > 0)
			writer.println(indent + "   <ico path=\"" + fIcoFilePath + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(indent + "   <bmp"); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_16_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_16_LOW, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_32_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_32_LOW, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_48_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_48_LOW, writer); //$NON-NLS-1$
		writer.println(indent + "   />"); //$NON-NLS-1$
		writer.println(indent + "</win>"); //$NON-NLS-1$
	}

	private void writeSolaris(String indent, PrintWriter writer) {
		writer.print(indent + "<solaris"); //$NON-NLS-1$
		writeIcon(indent + "   ", SOLARIS_LARGE, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", SOLARIS_MEDIUM, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", SOLARIS_SMALL, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", SOLARIS_TINY, writer); //$NON-NLS-1$
		writer.println(indent + "/>"); //$NON-NLS-1$
	}
	
	private void writeIcon(String indent, String iconId, PrintWriter writer) {
		String icon = (String)fIcons.get(iconId);
		if (icon != null && icon.length() > 0) {
			writer.println();
			writer.print(indent + "   " + iconId + "=\"" + icon + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
	}

	private void writeMac(String indent, PrintWriter writer) {
		String icon = (String)fIcons.get(MACOSX_ICON);
		if (icon != null && icon.length() > 0)
			writer.println(indent + "<macosx " + MACOSX_ICON + "=\"" + icon + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void writeLinux(String indent, PrintWriter writer) {
		String icon = (String)fIcons.get(LINUX_ICON);
		if (icon != null && icon.length() > 0)
			writer.println(indent + "<linux " + LINUX_ICON + "=\"" + icon + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
