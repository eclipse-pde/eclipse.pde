package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class StripMapper extends Mapper implements FileNameMapper {
	Path[] from;

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
	if (from.length == 0)
		return new String[] {path};

	IPath original = new Path (path);
	for (int i = 0; i < from.length; i++) {
		// if the pattern matches exactly and is not for a dir, the mapped filename
		// should be returned as the simple file name.
		if (from[i].removeTrailingSeparator().equals(original))
			if (from[i].hasTrailingSeparator())
				return null;
			else
				return new String[] {original.lastSegment()};
		// if the pattern is a prefix of the original, remove the pattern
		// and return the rest
		if (from[i].isPrefixOf(original)) 
			return new String[] {original.removeFirstSegments(from[i].segmentCount()).toOSString()};
	}
	return new String[] {path};
}
/**
 * @see FileNameMapper#setFrom(String)
 */
public void setFrom(String value) {
	if (value == null || value.trim().equals(""))
		from = new Path[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(value, ","); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals("") && token.charAt(0) != '/')
			result.add(new Path(token));
	}
	from = (Path[]) result.toArray(new Path[result.size()]);
}

/**
 * @see FileNameMapper#setTo(String)
 */
public void setTo(String value) {
}
}

