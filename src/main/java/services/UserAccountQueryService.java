package services;

import com.vmware.xenon.common.*;
import com.vmware.xenon.services.common.QueryTask;
import com.vmware.xenon.services.common.ServiceUriPaths;
import com.vmware.xenon.services.common.TaskService;

import java.net.URI;

/**
 * Created by sbai on 5/19/16.
 */
public class UserAccountQueryService
        extends TaskService<UserAccountQueryService.UserAccountQueryServiceState> {

    public static final String FACTORY_LINK = "user-accounts-search";

    public static FactoryService createFactory() {
        return FactoryService.create(UserAccountQueryService.class);
    }

    public static class UserAccountQueryServiceState extends TaskService.TaskServiceState {
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public TaskState state;
        public QueryTask queryTask;
    }

    public UserAccountQueryService() {
        super(UserAccountQueryServiceState.class);
    }

    @Override
    public void handlePatch(Operation patch) {
        UserAccountQueryServiceState body = patch.getBody(UserAccountQueryServiceState.class);
        UserAccountQueryServiceState current = getState(patch);

        validateTransition(patch, current, body);


    }

    protected UserAccountQueryServiceState validateStartPost(Operation startPost) {
        UserAccountQueryServiceState task = super.validateStartPost(startPost);

        if (task != null) {
            if (ServiceHost.isServiceCreate(startPost)) {
                if (task.state != null) {
                    startPost.fail(new IllegalArgumentException("Do not specify task state"));
                    return null;
                }
            }
        }

        return task;
    }

    protected boolean validateTransition(Operation patch, UserAccountQueryServiceState current,
                                         UserAccountQueryServiceState body) {
        if (!super.validateTransition(patch, current, body)) {
            logInfo("Super transition validation fail:\n%s\n\n%s", current.toString(), body
                    .toString());
        } else {
            if (current != null && current.state != null) {
                logInfo("The current state is %s", current.state.toString());
            }
            if (current != null && current.taskInfo != null && current.taskInfo.stage != null) {
                logInfo("The actual current task stage is %s", current.taskInfo.stage.toString());
            }
            if (body != null && body.state != null) {
                logInfo("The body state is %s", body.state.toString());
            }
            if (body != null && body.taskInfo != null && body.taskInfo.stage != null) {
                logInfo("The request body task stage is %s", current.taskInfo.stage.toString());
            }
        }

        return true;
    }

    protected void initializeState(UserAccountQueryServiceState task, Operation taskOperation) {
        task.state = TaskState.create();
        super.initializeState(task, taskOperation);
    }

    private void queryUserName(String userName, Operation patch) {
        QueryTask.Query userNameQuery = QueryTask.Query.Builder.create()
                .addFieldClause(ServiceDocument.FIELD_NAME_KIND, Utils.buildKind
                        (UserAccountService.UserAccountServiceState.class))
                .addFieldClause(UserAccountService.UserAccountServiceState.FIELD_NAME_USERNAME,
                        userName)
                .build();

        QueryTask queryTask = QueryTask.Builder.createDirectTask()
                .setQuery(userNameQuery)
                .build();

        URI queryTaskUri = UriUtils.buildUri(getHost(), ServiceUriPaths.CORE_QUERY_TASKS);

        Operation postQuery = Operation.createPost(queryTaskUri)
                .setBody(queryTask)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        logInfo("User name query fail: %s\n%s", userName, error.toString());
                    } else {
                        QueryTask queryResponse = operation.getBody(QueryTask.class);
                        patch.setBody(queryResponse);
                    }
                });
        
        getHost().sendRequest(postQuery);
    }

}
