package org.eclipse.pde.internal.core;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.eclipse.core.runtime.Path;

public class StripMapper extends Mapper implements FileNameMapper {
	int segmentsToStrip = 0;
	
public StripMapper(Project p) {
	super(p);
}
public FileNameMapper getImplementation() {
	return this;
}
/**
 * @see FileNameMapper#mapFileName(String)
 */
public String[] mapFileName(String path) {
	Path original = new Path (path);
	if (original.segmentCount() < 2)
		return new String[] {path};
	return new String[] {original.removeFirstSegments(segmentsToStrip).toOSString()};
}
/**
 * @see FileNameMapper#setFrom(String)
 */
public void setFrom(String value) {
	// do nothing
}

/**
 * @see FileNameMapper#setTo(String)
 */
public void setTo(String value) {
	segmentsToStrip = Integer.parseInt(value);
}
}

