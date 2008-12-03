/**
 * 
 */
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;

public class GatheringComputer implements IPathComputer {
	private final LinkedHashMap filesMap = new LinkedHashMap();

	public IPath computePath(File source) {
		String prefix = (String) filesMap.get(source);

		IPath result = new Path(source.getAbsolutePath());
		IPath rootPath = new Path(prefix);
		result = result.removeFirstSegments(rootPath.matchingFirstSegments(result));
		return result.setDevice(null);
	}

	public void reset() {
		// nothing

	}

	public void addAll(GatheringComputer computer) {
		filesMap.putAll(computer.filesMap);
	}

	public void addFiles(String prefix, String[] files) {
		for (int i = 0; i < files.length; i++) {
			filesMap.put(new File(prefix, files[i]), prefix);
		}
	}

	public void addFile(String prefix, String file) {
		filesMap.put(new File(prefix, file), prefix);
	}

	public File[] getFiles() {
		Set keys = filesMap.keySet();
		return (File[]) keys.toArray(new File[keys.size()]);
	}
}