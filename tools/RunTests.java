import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

// Runs every JUnit test of the Toolbox without Gradle (see check_without_gradle.sh). Prints "tests: N passed, M failed".
public final class RunTests {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("com.example.chartoolbox")).build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);
        TestExecutionSummary s = listener.getSummary();
        if (s.getTotalFailureCount() > 0) s.printFailuresTo(new PrintWriter(System.out), 12);
        System.out.println("tests: " + s.getTestsSucceededCount() + " passed, " + s.getTestsFailedCount() + " failed");
        System.exit(s.getTotalFailureCount() > 0 ? 1 : 0);
    }
}
