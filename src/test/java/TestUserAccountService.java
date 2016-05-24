import com.vmware.xenon.common.BasicReusableHostTestCase;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.UriUtils;

import org.junit.Before;
import org.junit.Test;

import services.UserAccountService;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by sbai on 5/24/16.
 * Validate that User Account Service runs correctly and can deal with all kinds of
 * restful apis correctly
 */
public class TestUserAccountService extends BasicReusableHostTestCase {

    private static final String INITIAL_USER_ACCOUNT_EMAIL = "test@test.com";
    private static final String INITIAL_USER_ACCOUNT_USER_NAME = "test4userName";
    private static final String INITIAL_USER_ACCOUNT_PASSWORD = "test4password";
    private static final String USER_ACCOUNT_TAG = "test4tag";
    private static final String USER_ACCOUNT_KEY = "test4key";
    private static final String USER_ACCOUNT_VALUE = "test4value";
    private static final String USER_ACCOUNT_USER_NAME = "example4changeUserName";
    private static final String USER_ACCOUNT_EMAIL = "example@example.com";

    @Before
    public void setUp() throws Exception {
        try {
            if (host.getServiceStage(UserAccountService.FACTORY_LINK) == null) {
                host.startServiceAndWait(UserAccountService.createFactory(), UserAccountService
                        .FACTORY_LINK, null);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCRUD() throws Throwable {
        URI factoryUri = UriUtils.buildUri(host, UserAccountService.FACTORY_LINK);
        UserAccountService.UserAccountServiceState userAccountServiceState = new
                UserAccountService.UserAccountServiceState();

        userAccountServiceState.email = INITIAL_USER_ACCOUNT_EMAIL;
        userAccountServiceState.userName = INITIAL_USER_ACCOUNT_USER_NAME;
        userAccountServiceState.password = INITIAL_USER_ACCOUNT_PASSWORD;
        userAccountServiceState.documentSelfLink = UUID.randomUUID().toString();
        URI childUri = UriUtils.buildUri(host, UserAccountService.FACTORY_LINK + "/" +
                userAccountServiceState.documentSelfLink);

        testCreateService(factoryUri, userAccountServiceState);
        testPost(childUri);
        testPatch(childUri);
        testPut(childUri);
        testDelete(childUri);
    }

    private void testCreateService(URI factoryUri, UserAccountService.UserAccountServiceState
            userAccountServiceState) throws Throwable {
        UserAccountService.UserAccountServiceState[] responses = new UserAccountService
                .UserAccountServiceState[1];
        Operation post  = Operation
                .createPost(factoryUri)
                .setBody(userAccountServiceState)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        responses[0] = operation.getBody(UserAccountService
                                .UserAccountServiceState.class);
                        host.completeIteration();
                    }
                });
        host.testStart(1);
        host.send(post);
        host.testWait();

        assertEquals(INITIAL_USER_ACCOUNT_EMAIL, responses[0].email);
        assertEquals(INITIAL_USER_ACCOUNT_USER_NAME, responses[0].userName);
        assertEquals(INITIAL_USER_ACCOUNT_PASSWORD, responses[0].password);
    }

    private void testPost(URI childUri) throws Throwable {
        UserAccountService.UserAccountServiceState[] responses = new UserAccountService
                .UserAccountServiceState[1];
        UserAccountService.UserAccountServiceState userAccountServiceState = new
                UserAccountService.UserAccountServiceState();
        userAccountServiceState.tags = new HashSet<>();
        userAccountServiceState.keyValues = new HashMap<>();
        userAccountServiceState.tags.add(USER_ACCOUNT_TAG);
        userAccountServiceState.keyValues.put(USER_ACCOUNT_KEY, USER_ACCOUNT_VALUE);

        Operation post = Operation
                .createPost(childUri)
                .setBody(userAccountServiceState)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        responses[0] = operation.getBody(UserAccountService.UserAccountServiceState
                                .class);
                        host.completeIteration();
                    }
                });
        host.testStart(1);
        host.send(post);
        host.testWait();

        assertNotEquals(responses[0].tags, null);
        assertEquals(responses[0].tags.size(), 1);
        assertEquals(responses[0].tags.iterator().next(), USER_ACCOUNT_TAG);
        assertNotEquals(responses[0].keyValues, null);
        assertEquals(responses[0].keyValues.size(), 1);
        assertEquals(responses[0].keyValues.keySet().iterator().next(), USER_ACCOUNT_KEY);
        assertEquals(responses[0].keyValues.values().iterator().next(), USER_ACCOUNT_VALUE);
    }

    private void testPatch(URI childUri) throws Throwable {
        UserAccountService.UserAccountServiceState[] responses = new UserAccountService
                .UserAccountServiceState[1];
        UserAccountService.UserAccountServiceState userAccountServiceState = new
                UserAccountService.UserAccountServiceState();
        userAccountServiceState.userName = USER_ACCOUNT_USER_NAME;

        Operation patch = Operation
                .createPatch(childUri)
                .setBody(userAccountServiceState)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        responses[0] = operation.getBody(UserAccountService
                                .UserAccountServiceState.class);
                        host.completeIteration();
                    }
                });
        host.testStart(1);
        host.send(patch);
        host.testWait();

        assertEquals(responses[0].userName, USER_ACCOUNT_USER_NAME);
        assertEquals(responses[0].email, INITIAL_USER_ACCOUNT_EMAIL);
        assertEquals(responses[0].password, INITIAL_USER_ACCOUNT_PASSWORD);
    }

    private void testPut(URI childUri) throws Throwable {
        UserAccountService.UserAccountServiceState[] responses = new UserAccountService
                .UserAccountServiceState[1];
        UserAccountService.UserAccountServiceState userAccountServiceState = new
                UserAccountService.UserAccountServiceState();
        userAccountServiceState.email = USER_ACCOUNT_EMAIL;

        Operation put = Operation
                .createPut(childUri)
                .setBody(userAccountServiceState)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        responses[0] = operation.getBody(UserAccountService
                                .UserAccountServiceState.class);
                        host.completeIteration();
                    }
                });
        host.testStart(1);
        host.send(put);
        host.testWait();

        assertEquals(responses[0].userName, null);
        assertEquals(responses[0].email, USER_ACCOUNT_EMAIL);
        assertEquals(responses[0].password, null);
    }

    private void testDelete(URI childUri) throws Throwable {
        Operation delete = Operation
                .createDelete(childUri)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        host.failIteration(error);
                    } else {
                        host.completeIteration();
                    }
                });
        host.testStart(1);
        host.send(delete);
        host.testWait();
    }
}
