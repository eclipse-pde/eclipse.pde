package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.runtime.Path;

public class PluginPathFinder {
	

	private static String[] getLinks(
		String platformHome,
		HashMap eclipseHomeVariables) {
		ArrayList result = new ArrayList();
		String prefix = new Path(platformHome).removeLastSegments(1).toString();
		File file = new File(platformHome + Path.SEPARATOR + "links");

		File[] linkFiles = new File[0];
		if (file.exists() && file.isDirectory()) {
			linkFiles = file.listFiles();
		}
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				Properties properties = new Properties();
				try {
					FileInputStream fis = new FileInputStream(linkFiles[i]);
					properties.load(fis);
					fis.close();
					String path = properties.getProperty("path");
					if (path != null) {
						if (!new Path(path).isAbsolute())
							path = prefix + Path.SEPARATOR + path;
						path += Path.SEPARATOR
							+ "eclipse"
							+ Path.SEPARATOR
							+ "plugins";
						if (new File(path).exists()) {
							String variable =
								PDECore.ECLIPSE_HOME_VARIABLE
									+ "_"
									+ linkFiles[i].getName().toUpperCase().replace(
										'.',
										'_');
							eclipseHomeVariables.put(
								variable,
								new Path(path).removeLastSegments(1));
							result.add(path);
						}
					}
				} catch (IOException e) {
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static String[] getPluginPaths(
		String platformHome,
		HashMap eclipseHomeVariables) {
		eclipseHomeVariables.clear();
		eclipseHomeVariables.put(PDECore.ECLIPSE_HOME_VARIABLE, platformHome);
		String[] links = getLinks(platformHome, eclipseHomeVariables);

		String[] paths = new String[links.length + 1];
		paths[0] = platformHome + File.separator + "plugins";
		if (links.length > 0) {
			System.arraycopy(links, 0, paths, 1, links.length);
		}

		return paths;
	}

}
