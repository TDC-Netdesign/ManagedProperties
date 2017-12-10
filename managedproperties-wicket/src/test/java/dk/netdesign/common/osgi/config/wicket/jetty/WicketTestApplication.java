package dk.netdesign.common.osgi.config.wicket.jetty;

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.*;
import dk.netdesign.common.osgi.config.service.HandlerFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.wicket.*;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.protocol.http.WebApplication;

import java.util.Map;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;

public class WicketTestApplication extends WebApplication {

    private ManagedPropertiesFactory factory;

    private ManagedPropertiesProvider provider = new ManagedPropertiesProvider(null) {
        @Override
        public void persistConfiguration(Map<String, Object> newConfiguration) throws InvocationException {

        }

        @Override
        public Class getReturnType(String configID) throws UnknownValueException {
            return String.class;
        }

        @Override
        public void start() throws Exception {

        }

        @Override
        public void stop() throws Exception {

        }
    };

    @Override
    public Class<? extends Page> getHomePage() {

        return ConfiguredPage.class;

    }

    private TestConfigurationItemFactory testConfigurationItemFactory = new TestConfigurationItemFactory();

    @Override
    protected void init() {
        super.init();

        getComponentInstantiationListeners().add(new CSSBehaviorListener());

        HandlerFactory handlerfactory = new HandlerFactory() {

            @Override
            public <E> ManagedPropertiesProvider getProvider(Class<? super E> configurationType, ManagedPropertiesController controller, E defaults) throws InvocationException, InvalidTypeException, InvalidMethodException, DoubleIDException {
                System.out.println("Adding " + configurationType + "->" + controller);
                testConfigurationItemFactory.addConfigItem(configurationType, ManagedPropertiesFactory.castToProxy(configurationType, controller));
                return provider;
            }
        };

        factory = new ManagedPropertiesFactory(handlerfactory, null, null);
        try {
            factory.register(SetterConfig.class);
        } catch (ManagedPropertiesException e) {
            e.printStackTrace();
        }

        getComponentInstantiationListeners().add(new IComponentInstantiationListener() {
            @Override
            public void onInstantiation(Component component) {
                if (ConfiguredPage.class.isInstance(component)) {
                    ConfiguredPage configuredPage = ConfiguredPage.class.cast(component);
                    configuredPage.setFactory(testConfigurationItemFactory);
                }
                if (InjectingConfigurationPage.class.isAssignableFrom(component.getClass())) {
                    InjectingConfigurationPage injectingConfigurationPage = InjectingConfigurationPage.class.cast(component);
                    injectingConfigurationPage.setFactory(testConfigurationItemFactory);
                }

            }

        });

    }
    
    private static class CSSBehaviorListener implements IComponentInstantiationListener{
        
        @Override
            public void onInstantiation(Component component) {
                if (component instanceof WebPage) {
                    component.add(new CSSBehavior());
                }

            }
        
    }

    private static class CSSBehavior extends Behavior {

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            response.render(CssHeaderItem.forUrl("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"));
            response.render(CssHeaderItem.forUrl("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"));
            response.render(JavaScriptHeaderItem.forUrl("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"));
        }

    }

}
