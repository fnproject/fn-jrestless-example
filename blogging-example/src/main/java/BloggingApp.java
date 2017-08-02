import org.glassfish.jersey.server.ResourceConfig;

public class BloggingApp extends OracleFunctionsRequestObjectHandler {

    public BloggingApp() {

        ResourceConfig config = new ResourceConfig();
        config.register(BloggingResource.class);
        config.register(OracleFeature.class);

        init(config);

        start();
    }

}
