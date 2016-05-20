package services;

import com.vmware.xenon.common.*;

import java.util.*;

/**
 * Created by sbai on 5/18/16.
 * This is the factory service used to manage user account.
 */
public class UserAccountService extends StatefulService {

    public static final String FACTORY_LINK = "/user-accounts";

    public static FactoryService createFactory() {
        return FactoryService.create(UserAccountService.class);
    }

    public static class UserAccountServiceState extends ServiceDocument {
        public static final String FIELD_NAME_USERNAME = "userName";
        public String userName;
        public String email;
        public String password;
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.OPTIONAL)
        public Set<String> tags = new HashSet<>();
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.OPTIONAL)
        public Map<String, String> keyValues = new HashMap<>();
    }

    public UserAccountService() {
        super(UserAccountServiceState.class);
        toggleOption(ServiceOption.PERSISTENCE, true);
//        toggleOption(ServiceOption.REPLICATION, true);
//        toggleOption(ServiceOption.INSTRUMENTATION, true);
//        toggleOption(ServiceOption.OWNER_SELECTION, true);
    }

    @Override
    public void handleStart(Operation startPost) {
        System.out.println("In Start Handler");
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
    public void handlePost(Operation post) {
        System.out.println("In Post Handler");

        addToState(post);

        post.complete();
    }

    @Override
    public void handlePatch(Operation patch) {
        System.out.println("In Patch Handler");

        updateState(patch, false);

        patch.complete();
    }

    @Override
    public void handlePut(Operation put) {
        System.out.println("In Put Handler");

        updateState(put, true);

        put.complete();
    }

    @Override
    public void handleDelete(Operation delete) {
        System.out.println("In Delete Handler");

        if (delete.hasBody()) {
            UserAccountServiceState body = delete.getBody(UserAccountServiceState.class);
            UserAccountServiceState current = getState(delete);
            if (current != null && body.documentExpirationTimeMicros > 0) {
                current.documentExpirationTimeMicros = body.documentExpirationTimeMicros;
            }
        }

        delete.complete();
    }

    private void addToState(Operation add) {
        UserAccountServiceState body = add.getBody(UserAccountServiceState.class);
        UserAccountServiceState current = getState(add);

        if (current != null) {
            updateTags(body, current);
            updateKeyValues(body, current);
        }

        add.setBody(current);
    }

    private void updateState(Operation update, boolean force) {
        UserAccountServiceState body = update.getBody(UserAccountServiceState.class);
        UserAccountServiceState current = getState(update);

        if (current != null) {
            updateUserName(body, current, force);
            updateEmail(body, current, force);
            updatePassword(body, current, force);
        }

        update.setBody(current);
    }

    private void updateKeyValues(UserAccountServiceState body, UserAccountServiceState current) {
        if (body != null && body.keyValues != null && !body.keyValues.isEmpty()) {
            for (Map.Entry<String, String> entry : body.keyValues.entrySet()) {
                current.keyValues.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void updateTags(UserAccountServiceState body, UserAccountServiceState current) {
        if (body != null && body.tags != null && !body.tags.isEmpty()) {
            for (String tag : body.tags) {
                current.tags.add(tag);
            }
        }
    }

    private void updateUserName(UserAccountServiceState body, UserAccountServiceState
            current, boolean force) {
        if (body != null && body.userName != null) {
            current.userName = body.userName;
        } else if (force) {
            current.userName = null;
        }
    }

    private void updateEmail(UserAccountServiceState body, UserAccountServiceState
            current, boolean force) {
        if (body != null && body.email != null) {
            current.email = body.email;
        } else if (force) {
            current.email = null;
        }
    }

    private void updatePassword(UserAccountServiceState body, UserAccountServiceState
            current, boolean force) {
        if (body != null && body.password != null) {
            current.password = body.password;
        } else if (force) {
            current.password = null;
        }
    }
}
