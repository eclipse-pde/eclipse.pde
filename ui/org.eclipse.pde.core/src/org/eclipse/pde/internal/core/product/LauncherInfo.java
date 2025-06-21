/*******************************************************************************
 *  Copyright (c) 2005, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     SAP SE - support macOS bundle URL types
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IMacBundleUrlType;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LauncherInfo extends ProductObject implements ILauncherInfo {

	private static final long serialVersionUID = 1L;
	private boolean fUseIcoFile;
	private final Map<String, String> fIcons = new HashMap<>();
	private String fLauncherName;
	private final TreeMap<String, IMacBundleUrlType> fMacBundleUrlTypes = new TreeMap<>();

	public LauncherInfo(IProductModel model) {
		super(model);
	}

	@Override
	public String getLauncherName() {
		return fLauncherName;
	}

	@Override
	public void setLauncherName(String name) {
		String old = fLauncherName;
		fLauncherName = name;
		if (isEditable()) {
			firePropertyChanged(P_LAUNCHER, old, fLauncherName);
		}
	}

	@Override
	public void setIconPath(String iconId, String path) {
		if (path == null) {
			path = ""; //$NON-NLS-1$
		}
		String old = fIcons.get(iconId);
		fIcons.put(iconId, path);
		if (isEditable()) {
			firePropertyChanged(iconId, old, path);
		}
	}

	@Override
	public String getIconPath(String iconId) {
		return fIcons.get(iconId);
	}

	@Override
	public boolean usesWinIcoFile() {
		return fUseIcoFile;
	}

	@Override
	public void setUseWinIcoFile(boolean use) {
		boolean old = fUseIcoFile;
		fUseIcoFile = use;
		if (isEditable()) {
			firePropertyChanged(P_USE_ICO, Boolean.toString(old), Boolean.toString(fUseIcoFile));
		}
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			fLauncherName = ((Element) node).getAttribute("name"); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					switch (name) {
					case "freebsd": //$NON-NLS-1$
						parseFreeBSD((Element) child);
						break;
					case "linux": //$NON-NLS-1$
						parseLinux((Element) child);
						break;
					case "macosx": //$NON-NLS-1$
						parseMac((Element) child);
						break;
					case "win": //$NON-NLS-1$
						parseWin((Element) child);
						break;
					default:
						break;
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
				Element child = (Element) children.item(i);
				String name = child.getNodeName();
				if (name.equals("ico")) { //$NON-NLS-1$
					fIcons.put(P_ICO_PATH, child.getAttribute("path")); //$NON-NLS-1$
				} else if (name.equals("bmp")) { //$NON-NLS-1$
					fIcons.put(WIN32_16_HIGH, child.getAttribute(WIN32_16_HIGH));
					fIcons.put(WIN32_16_LOW, child.getAttribute(WIN32_16_LOW));
					fIcons.put(WIN32_32_HIGH, child.getAttribute(WIN32_32_HIGH));
					fIcons.put(WIN32_32_LOW, child.getAttribute(WIN32_32_LOW));
					fIcons.put(WIN32_48_HIGH, child.getAttribute(WIN32_48_HIGH));
					fIcons.put(WIN32_48_LOW, child.getAttribute(WIN32_48_LOW));
					fIcons.put(WIN32_256_HIGH, child.getAttribute(WIN32_256_HIGH));
				}
			}
		}
	}

	private void parseMac(Element element) {
		fIcons.put(MACOSX_ICON, element.getAttribute("icon")); //$NON-NLS-1$
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Node child = children.item(i);
				String name = child.getNodeName();
				if (name.equals("bundleUrlTypes")) { //$NON-NLS-1$
					NodeList grandChildren = child.getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						Node grandChild = grandChildren.item(j);
						if (grandChild.getNodeType() == Node.ELEMENT_NODE) {
							if (grandChild.getNodeName().equals("bundleUrlType")) { //$NON-NLS-1$
								IMacBundleUrlType bundleUrlType = getModel().getFactory().createMacBundleUrlType();
								bundleUrlType.parse(grandChild);
								fMacBundleUrlTypes.put(bundleUrlType.getScheme(), bundleUrlType);
							}
						}
					}
				}
			}
		}
	}

	private void parseFreeBSD(Element element) {
		fIcons.put(FREEBSD_ICON, element.getAttribute("icon")); //$NON-NLS-1$
	}

	private void parseLinux(Element element) {
		fIcons.put(LINUX_ICON, element.getAttribute("icon")); //$NON-NLS-1$
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<launcher"); //$NON-NLS-1$
		if (fLauncherName != null && fLauncherName.length() > 0) {
			writer.print(" name=\"" + fLauncherName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$

		writeFreeBSD(indent + "   ", writer); //$NON-NLS-1$
		writeLinux(indent + "   ", writer); //$NON-NLS-1$
		writeMac(indent + "   ", writer); //$NON-NLS-1$
		writerWin(indent + "   ", writer); //$NON-NLS-1$
		writer.println(indent + "</launcher>"); //$NON-NLS-1$
	}

	private void writerWin(String indent, PrintWriter writer) {
		writer.println(indent + "<win " + P_USE_ICO + "=\"" + fUseIcoFile + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String path = fIcons.get(P_ICO_PATH);
		if (path != null && path.length() > 0) {
			writer.println(indent + "   <ico path=\"" + getWritableString(path) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.print(indent + "   <bmp"); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_16_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_16_LOW, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_32_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_32_LOW, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_48_HIGH, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_48_LOW, writer); //$NON-NLS-1$
		writeIcon(indent + "   ", WIN32_256_HIGH, writer); //$NON-NLS-1$
		writer.println("/>"); //$NON-NLS-1$
		writer.println(indent + "</win>"); //$NON-NLS-1$
	}

	private void writeIcon(String indent, String iconId, PrintWriter writer) {
		String icon = fIcons.get(iconId);
		if (icon != null && icon.length() > 0) {
			writer.println();
			writer.print(indent + "   " + iconId + "=\"" + getWritableString(icon) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	private void writeMac(String indent, PrintWriter writer) {
		String icon = fIcons.get(MACOSX_ICON);
		if (icon != null && icon.length() > 0 || !fMacBundleUrlTypes.isEmpty()) {
			writer.print(indent + "<macosx"); //$NON-NLS-1$
			if (icon != null && icon.length() > 0) {
				writer.print(" icon=\"" + getWritableString(icon) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (fMacBundleUrlTypes.isEmpty()) {
				writer.println("/>"); //$NON-NLS-1$
			} else {
				writer.println(">"); //$NON-NLS-1$
				writer.println(indent + "   <bundleUrlTypes>"); //$NON-NLS-1$
				for (IMacBundleUrlType bundleUrlType : fMacBundleUrlTypes.values()) {
					bundleUrlType.write(indent + "      ", writer); //$NON-NLS-1$
				}
				writer.println(indent + "   </bundleUrlTypes>"); //$NON-NLS-1$
				writer.println(indent + "</macosx>"); //$NON-NLS-1$
			}
		}
	}

	private void writeFreeBSD(String indent, PrintWriter writer) {
		String icon = fIcons.get(FREEBSD_ICON);
		if (icon != null && icon.length() > 0) {
			writer.println(indent + "<freebsd icon=\"" + getWritableString(icon) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void writeLinux(String indent, PrintWriter writer) {
		String icon = fIcons.get(LINUX_ICON);
		if (icon != null && icon.length() > 0) {
			writer.println(indent + "<linux icon=\"" + getWritableString(icon) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public List<IMacBundleUrlType> getMacBundleUrlTypes() {
		return List.copyOf(fMacBundleUrlTypes.values());
	}

	@Override
	public void addMacBundleUrlTypes(List<IMacBundleUrlType> bundleUrlTypes) {
		IMacBundleUrlType[] addedUrlSchemes = bundleUrlTypes.stream().map(urlType -> {
			if (urlType != null) {
				String scheme = urlType.getScheme();
				if (scheme != null && !fMacBundleUrlTypes.containsKey(scheme)) {
					urlType.setModel(getModel());
					fMacBundleUrlTypes.put(scheme, urlType);
					return urlType;
				}
			}
			return null;
		}).filter(Objects::nonNull).toArray(IMacBundleUrlType[]::new);
		if (addedUrlSchemes.length > 0 && isEditable()) {
			fireStructureChanged(addedUrlSchemes, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void removeMacBundleUrlTypes(List<IMacBundleUrlType> bundleUrlTypes) {
		IProductObject[] removedUrlSchemes = bundleUrlTypes.stream() //
				.map(IMacBundleUrlType::getScheme).map(fMacBundleUrlTypes::remove) //
				.filter(Objects::nonNull).toArray(IProductObject[]::new);

		if (removedUrlSchemes.length > 0 && isEditable()) {
			fireStructureChanged(removedUrlSchemes, IModelChangedEvent.REMOVE);
		}
	}

}
