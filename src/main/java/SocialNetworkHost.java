import com.vmware.xenon.common.ServiceHost;
import com.vmware.xenon.services.common.RootNamespaceService;
import services.UserAccountService;

import java.util.logging.Level;

/**
 * Created by sbai on 5/18/16.
 * This is service host, which will run other services for the social network.
 */
public class SocialNetworkHost extends ServiceHost {

    /**
     * Just copy-paste from example service host.
     * @param args input arguments
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable {
        SocialNetworkHost h = new SocialNetworkHost();
        h.initialize(args);
        h.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            h.log(Level.WARNING, "Host stopping ...");
            h.stop();
            h.log(Level.WARNING, "Host is stopped");
        }));
    }

    /**
     * Start all other services here
     * @return return this host service
     * @throws Throwable
     */
    @Override
    public ServiceHost start() throws Throwable {
        super.start();

        startDefaultCoreServicesSynchronously();

        super.startFactory(UserAccountService.class, UserAccountService::createFactory);

        super.startService(new RootNamespaceService());

        return this;
    }
}
