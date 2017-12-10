package dk.netdesign.common.osgi.config.wicket;

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;

import java.util.HashMap;
import java.util.Map;

public class TestConfigurationItemFactory extends ConfigurationItemFactory{
    Map<String, Object> configItemsByID = new HashMap<>();

    public TestConfigurationItemFactory() {

    }

    public void addConfigItem(Class<?> type, Object configItem) throws InvalidTypeException {

        String id = ManagedPropertiesController.getDefinitionID(type);

        configItemsByID.put(id, configItem);
    }

    @Override
    protected <E> E retrieveConfigurationItem(Class<E> configurationItem) {
        try {
            E config = (E) configItemsByID.get(ManagedPropertiesController.getDefinitionID(configurationItem));
            System.out.println("Returning "+config+" for "+configurationItem);
            return config;
        } catch (InvalidTypeException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected Object retrieveConfigurationItem(String configurationID) {
        Object config = configItemsByID.get(configurationID);
        System.out.println("Returning "+config+" for "+configurationID);
        return config;
    }

    @Override
    public String toString() {
        return "TestConfigurationItemFactory{" + "configItemsByID=" + configItemsByID + '}';
    }





}
