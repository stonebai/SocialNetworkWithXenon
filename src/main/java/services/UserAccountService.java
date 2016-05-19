package services;

import com.vmware.xenon.common.*;
import com.vmware.xenon.services.common.ExampleService;

/**
 * Created by sbai on 5/18/16.
 * This is the factory service used for managing user account.
 */
public class UserAccountService extends StatefulService {

    public static final String FACTORY_LINK = "/user-accounts";

    public static FactoryService createFactory() {
        return FactoryService.create(UserAccountService.class);
    }

    public static class UserAccountServiceState extends ServiceDocument {
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public String userName;
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.ID)
        public String email;
        public String password;
    }

    public UserAccountService() {
        super(UserAccountServiceState.class);
    }

    @Override
    public void handleStart(Operation startPost) {
        if (!startPost.hasBody()) {
            startPost.fail(new IllegalArgumentException("initial state is required"));
        } else {
            UserAccountServiceState s = startPost.getBody(UserAccountServiceState.class);
            if (s.email == null || s.password == null) {
                startPost.fail(new IllegalArgumentException("email and password are required"));
            } else {
                startPost.complete();
            }
        }
    }

    @Override
    public void handlePatch(Operation patch) {
        updateState(patch);

        patch.complete();
    }

    private void updateState(Operation update) {
        UserAccountServiceState body = getBody(update);

        if (body != null) {
            UserAccountServiceState current = getState(update);
            updateUserName(body, current);
            update.setBody(current);
        }
    }

    private void updateUserName(UserAccountServiceState body, UserAccountServiceState current) {
        if (body.userName != null) {
            current.userName = body.userName;
        }
    }
}
