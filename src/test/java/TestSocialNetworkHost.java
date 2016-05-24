import com.vmware.xenon.common.BasicReportTestCase;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocumentQueryResult;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.services.common.RootNamespaceService;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestSocialNetworkHost extends BasicReportTestCase {

    private TemporaryFolder tmpFolder = new TemporaryFolder();
    private SocialNetworkHost socialNetworkHost;

    @Test
    public void testState() throws Throwable {
        tmpFolder.create();
        try {
            startSocialNetworkHost();
            verifySocialNetworkHost();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopSocialNetworkHost();
            tmpFolder.delete();
        }
    }

    private void startSocialNetworkHost() throws Throwable {
        socialNetworkHost = new SocialNetworkHost();
        String bindAddress = "127.0.0.1";
        String hostId = UUID.randomUUID().toString();
        String[] args = {
                "--port=0",
                "--bindAddress=" + bindAddress,
                "--sandbox=" + this.tmpFolder.getRoot().getAbsolutePath(),
                "--id=" + hostId
        };

        socialNetworkHost.initialize(args);
        socialNetworkHost.start();

        assertEquals(bindAddress, this.socialNetworkHost.getPreferredAddress());
        assertEquals(bindAddress, this.socialNetworkHost.getUri().getHost());
        assertEquals(hostId, this.socialNetworkHost.getId());
        assertEquals(this.socialNetworkHost.getUri(), this.socialNetworkHost.getPublicUri());
    }

    private void verifySocialNetworkHost() throws Throwable {
        host.waitForServiceAvailable(RootNamespaceService.SELF_LINK);

        URI rootUri = UriUtils.buildUri(socialNetworkHost, RootNamespaceService.class);
        host.testStart(1);

        Operation get = Operation
                .createGet(rootUri)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        validateRootResponse(operation);
                        host.completeIteration();
                    }
                });
        host.send(get);
        host.testWait();
    }

    private void validateRootResponse(Operation response) {
        ServiceDocumentQueryResult body = response.getBody(ServiceDocumentQueryResult.class);
        assertNotEquals(body, null);
        assertNotEquals(body.documentLinks, null);
        assertNotEquals(body.documentLinks.size(), 0);
    }

    private void stopSocialNetworkHost() {
        host.stop();
    }
}