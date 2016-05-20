package services;

import com.vmware.xenon.common.*;
import com.vmware.xenon.services.common.QueryTask;
import com.vmware.xenon.services.common.ServiceUriPaths;
import com.vmware.xenon.services.common.TaskService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by sbai on 5/19/16.
 */
public class UserAccountQueryService
        extends TaskService<UserAccountQueryService.UserAccountQueryServiceState> {

    public enum SubStage {
        QUERY, ADD_TAGS
    }

    public enum Type {
        QUERY_ONLY, QUERY_ADD_TAGS
    }

    public static final String FACTORY_LINK = "/user-accounts-search";

    public static FactoryService createFactory() {
        return FactoryService.create(UserAccountQueryService.class);
    }

    public static class UserAccountQueryServiceState extends TaskService.TaskServiceState {
        @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public SubStage stage;
        public Type type;
        public String userName;
        public QueryTask queryTask;
        public List<String> results;
    }

    public UserAccountQueryService() {
        super(UserAccountQueryServiceState.class);
//        toggleOption(ServiceOption.PERSISTENCE, true);
//        toggleOption(ServiceOption.REPLICATION, true);
//        toggleOption(ServiceOption.INSTRUMENTATION, true);
//        toggleOption(ServiceOption.OWNER_SELECTION, true);
    }

    @Override
    public void handleStart(Operation startPost) {
        if (startPost.hasBody()) {
            UserAccountQueryServiceState body = startPost.getBody(UserAccountQueryServiceState
                    .class);
            if (body.type != null && body.userName != null) {
                if (body.type == Type.QUERY_ONLY) {
                    syncQueryUserName(body.userName, startPost);
                } else if (body.type == Type.QUERY_ADD_TAGS){
                    startPost.complete();
                    sendSelfPatch(body, TaskState.TaskStage.STARTED, subStageSetter(SubStage.QUERY));
                } else {
                    startPost.fail(new IllegalArgumentException("Type: %s is not implemented"));
                }
            } else {
                if (body.userName != null) logInfo(body.userName);
                if (body.type != null) logInfo(body.type.toString());
                startPost.fail(new IllegalArgumentException("type and userNAme are required"));
            }

        } else {
            startPost.fail(new IllegalArgumentException("Initial state is required"));
        }
    }

    @Override
    public void handlePatch(Operation patch) {
        UserAccountQueryServiceState body = patch.getBody(UserAccountQueryServiceState.class);
        UserAccountQueryServiceState current = getState(patch);

        if (validateTransition(patch, current, body)) {
            updateState(current, body);
        }

        updateState(current, body);
        patch.complete();

        switch (body.taskInfo.stage) {
            case CREATED:
                logInfo("In created stage");
                break;
            case STARTED:
                logInfo("In started stage");
                handleSubstage(body);
                break;
            case CANCELLED:
                logInfo("In cancelled stage");
                break;
            case FINISHED:
                logInfo("In finished stage");
                break;
            case FAILED:
                logInfo("In failed stage");
                break;
            default:
                logInfo("Unexpected stage: %s", body.taskInfo.stage.toString());
                break;
        }
    }

    private void handleSubstage(UserAccountQueryServiceState body) {
        switch (body.stage) {
            case QUERY:
                logInfo("In stage query");
                asyncQueryUserName(body.userName, body);
                break;
            case ADD_TAGS:
                logInfo("In stage add tags");
                asyncAddTags(body);
                break;
            default:
                logInfo("Unexpected stage: %s", body.stage.toString());
                break;
        }
    }

    protected UserAccountQueryServiceState validateStartPost(Operation startPost) {
        UserAccountQueryServiceState task = super.validateStartPost(startPost);

        if (task != null) {
            if (ServiceHost.isServiceCreate(startPost)) {
                if (task.stage != null) {
                    startPost.fail(new IllegalArgumentException("Do not specify task state"));
                    return null;
                }
                if (task.queryTask != null) {
                    startPost.fail(new IllegalArgumentException("Do not specify task query"));
                    return null;
                }
            }
        }

        return task;
    }

    protected boolean validateTransition(Operation patch, UserAccountQueryServiceState current,
                                         UserAccountQueryServiceState body) {
        super.validateTransition(patch, current, body);
        if (body.taskInfo.stage == TaskState.TaskStage.STARTED && body.stage == null) {
            patch.fail(new IllegalArgumentException("Missing stage"));
            return false;
        }
        if (current.taskInfo != null && current.taskInfo.stage != null) {
            if (current.taskInfo.stage == TaskState.TaskStage.STARTED
                    && body.taskInfo.stage == TaskState.TaskStage.STARTED) {
                if (current.stage.ordinal() > body.stage.ordinal()) {
                    patch.fail(new IllegalArgumentException("Task stage cannot move backwards"));
                    return false;
                }
            }
        }

        return true;
    }

    protected void initializeState(UserAccountQueryServiceState task, Operation taskOperation) {
        task.stage = SubStage.QUERY;
        super.initializeState(task, taskOperation);
    }

    private void syncQueryUserName(String userName, Operation task) {
        QueryTask queryTask = generateUserNameQuery(userName);
        URI queryTaskUri = generateQueryURI();

        Operation postQuery = Operation.createPost(queryTaskUri)
                .setBody(queryTask)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        task.fail(new Throwable("Query Failed:\n" + error.toString()));
                    } else {
                        task.getBody(UserAccountQueryServiceState.class).results = operation
                                .getBody(QueryTask.class).results.documentLinks;
                        task.complete();
                    }
                });

        sendRequest(postQuery);
    }

    private void asyncQueryUserName(String userName, UserAccountQueryServiceState task) {
        QueryTask queryTask = generateUserNameQuery(userName);
        URI queryTaskUri = generateQueryURI();

        Operation postQuery = Operation.createPost(queryTaskUri)
                .setBody(queryTask)
                .setCompletion((operation, error) -> {
                    if (error != null) {
                        logInfo("User name query fail: %s\n%s", userName, error.toString());
                    } else {
                        task.queryTask = operation.getBody(QueryTask.class);
                        sendSelfPatch(task, TaskState.TaskStage.STARTED, subStageSetter(SubStage
                                .ADD_TAGS));
                    }
                });

        sendRequest(postQuery);
    }

    private void asyncAddTags(UserAccountQueryServiceState task) {
        if (task.queryTask.results == null) {
            sendSelfFailurePatch(task, "Query task service returned null results");
        } else if (task.queryTask.results.documentLinks == null) {
            sendSelfFailurePatch(task, "Query task service returned null documentLinks");
        } else if (task.queryTask.results.documentLinks.size() == 0) {
            sendSelfFailurePatch(task, "Query task service returned 0 documentLinks");
        } else {
            List<Operation> operations = new ArrayList<>();
            UserAccountService.UserAccountServiceState state = new UserAccountService
                    .UserAccountServiceState();
            state.tags.add("tag added by query service");
            for (String service : task.queryTask.results.documentLinks) {
                URI serviceUri = UriUtils.buildUri(getHost(), service);
                Operation operation = Operation.createPost(serviceUri);
                operation.setBody(state);
                operations.add(operation);
            }

            OperationJoin.create()
                    .setOperations(operations)
                    .setCompletion((operation, error) -> {
                        if (error != null && !error.isEmpty()) {
                            sendSelfFailurePatch(task, String.format("%d operations failed", error
                                    .size
                                    ()));
                        } else {
                            sendSelfPatch(task, TaskState.TaskStage.FINISHED, null);
                        }
                    }).sendWith(this);
        }
    }

    private QueryTask generateUserNameQuery(String userName) {
        QueryTask.Query.Builder builder = QueryTask.Query.Builder.create()
                .addKindFieldClause(UserAccountService.UserAccountServiceState.class)
                .addFieldClause(UserAccountService.UserAccountServiceState.FIELD_NAME_USERNAME,
                        userName);

        QueryTask.Query userNameQuery = builder.build();

        return QueryTask.Builder.createDirectTask()
                .setQuery(userNameQuery)
                .build();
    }

    private URI generateQueryURI() {
        return UriUtils.buildUri(getHost(), ServiceUriPaths.CORE_QUERY_TASKS);
    }

    private Consumer<UserAccountQueryServiceState> subStageSetter(SubStage substage) {
        return taskState -> taskState.stage = substage;
    }
}
