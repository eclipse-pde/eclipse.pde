import org.eclipse.pde.internal.build.tasks.PluginVersionReplaceTask;

public class Test1 {
	public static void main(String[] args) {
		PluginVersionReplaceTask replacer = new PluginVersionReplaceTask();
		replacer.setPluginFilePath("d:/tmp/plugin.xml");
		replacer.setVersionNumber("foo");
		replacer.execute();
	}
}
