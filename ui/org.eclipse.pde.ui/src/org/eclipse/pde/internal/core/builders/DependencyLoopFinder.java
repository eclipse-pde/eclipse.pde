package org.eclipse.pde.internal.core.builders;

/**
 * @version 	1.0
 * @author
 */

import org.eclipse.pde.core.plugin.*;
import java.util.Vector;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class DependencyLoopFinder {
	private static final String KEY_LOOP_NAME = "Builders.DependencyLoopFinder.loopName";	
	
	public static DependencyLoop [] findLoops(IPlugin root) {
		return findLoops(root, null);
	}

	public static DependencyLoop [] findLoops(IPlugin root, IPlugin [] candidates) {
		return findLoops(root, candidates, false);
	}
	
	public static DependencyLoop [] findLoops(IPlugin root, IPlugin [] candidates, boolean onlyCandidates) {
		Vector loops = new Vector();
		
		Vector path = new Vector();
		findLoops(loops, path, root, candidates, onlyCandidates);
		return (DependencyLoop[])loops.toArray(new DependencyLoop[loops.size()]);
	}
	
	private static void findLoops(Vector loops, Vector path, IPlugin subroot, IPlugin [] candidates, boolean onlyCandidates) {
		if (path.size()>0) {
			// test the path so far
			// is the subroot the same as root - if yes, that's it

			IPlugin root = (IPlugin)path.elementAt(0);
			if (isEquivalent(root, subroot)) {
				// our loop!!
				DependencyLoop loop = new DependencyLoop();
				loop.setMembers((IPlugin[])path.toArray(new IPlugin[path.size()]));
				String pattern = PDEPlugin.getResourceString(KEY_LOOP_NAME);
				int no = loops.size()+1;
				loop.setName(PDEPlugin.getFormattedMessage(pattern,(""+no)));
				loops.add(loop);
				return;
			}
			// is the subroot the same as any other node?
			// if yes, abort - local loop that is not ours
			for (int i=1; i<path.size(); i++) {
				IPlugin node = (IPlugin)path.elementAt(i);
				if (isEquivalent(subroot, node)) {
					// local loop
					return;
				}
			}
		}
		Vector newPath = path.size()>0?((Vector)path.clone()):path;	
		newPath.add(subroot);
		
		if (!onlyCandidates) {
			IPluginImport [] iimports = subroot.getImports();
			for (int i=0; i<iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				String id = iimport.getId();
				IPlugin child = PDEPlugin.getDefault().findPlugin(id);
				if (child!=null) {
					findLoops(loops, newPath, child, null, false);
				}	
			}
		}
		if (candidates!=null) {
			for (int i=0; i<candidates.length; i++) {
				IPlugin candidate = candidates[i];
				findLoops(loops, newPath, candidate, null, false);
			}
		}
	}
	
	private static boolean isEquivalent(IPlugin left, IPlugin right) {
		return left.getId().equals(right.getId());
	}
}