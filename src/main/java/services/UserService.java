package services;

import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.StatelessService;

/**
 * Created by sbai on 5/23/16.
 * This is a stateless service to manage user account
 */
public class UserService extends StatelessService {

    public static final String SELF_LINK = "/user";

    /**
     * The user account which should be the body of request
     */
    private static class UserAccount {
        public String userName;
        public String email;
        public String password;
    }

    @Override
    public void handlePatch(Operation patch) {
        if (patch.hasBody()) {
            printBody(patch.getBody(UserAccount.class));
            patch.complete();
        } else {
            patch.fail(new IllegalArgumentException("body is required"));
        }
    }

    @Override
    public void handleGet(Operation get) {
        get.setBody(new UserAccount());
        get.complete();
    }

    @Override
    public void handleStart(Operation startPost) {
        startPost.setBody(new UserAccount());
        startPost.complete();
    }

    @Override
    public void handlePost(Operation post) {
        if (post.hasBody()) {
            printBody(post.getBody(UserAccount.class));
            post.complete();
        } else {
            post.fail(new IllegalArgumentException("body is required"));
        }
    }

    @Override
    public void handleDelete(Operation delete) {
        delete.setBody(new UserAccount());
        delete.complete();
    }

    /**
     * print the request body
     * @param userAccount input the user account object obtained from request body
     */
    private void printBody(UserAccount userAccount) {
        System.out.println("User name: " + userAccount.userName);
        System.out.println("User email: " + userAccount.email);
        System.out.println("User password: " + userAccount.password);
    }
}
