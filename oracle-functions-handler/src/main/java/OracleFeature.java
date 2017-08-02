import com.jrestless.core.filter.ApplicationPathFilter;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class OracleFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(ApplicationPathFilter.class);
        return true;
    }
}
