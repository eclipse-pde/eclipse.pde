This PDE JUnit runtime is added to test-runtimes launched from an Eclipse workspace and allows JUnit tests to be run with an OSGi runtime.
It supports the following use cases:
1. Headless tests (no UI, no workbench)<br/>
  Runs NonUIThreadTestApplication with no testable object
2. e4 UI tests (e4 UI, no workbench)<br/>
  Runs NonUIThreadTestApplication with a testable object from e4 service
3. UI tests run in the non UI thread (UI, workbench)<br/>
  Runs NonUIThreadTestApplication with a testable object from e4 service or PlatformUI
4. UI tests run in the UI thread (UI, workbench)<br/>
  Runs UITestApplication with a testable object from e4 service or PlatformUI
5. Headless tests with no application (no UI, no workbench, no application)<br/>
  Runs directly with no application

If no pde.junit.runtime is available in the Target-Platform the one from the running Eclipse is added to the test-runtime.
Of course users can target older Eclipse versions (which for example require older Java-versions), 
which is why the requirements of this Plug-in should be as low as possible to make it resolve even in older runtimes.

If changes are made, one should ensure that the requirement's lower-bounds specified in the MANIFEST.MF are still valid.
