package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationTarget;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallback;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler;
import dk.netdesign.common.osgi.config.enhancement.PropertyActions;
import dk.netdesign.common.osgi.config.enhancement.PropertyConfig;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ManagedPropertiesController class is used as the central mechanism for
 * the link between the application and the configuration source. This class is
 * used as an InvocationHandler for the java.util.Proxy which is returned for
 * any registered interface.
 *
 * @author mnn
 */
public class ManagedPropertiesController implements InvocationHandler, ConfigurationTarget, ConfigurationCallbackHandler, PropertyActions, PropertyConfig {

    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesController.class);

    private Map<String, Object> config = new HashMap<>();
    private long nanosToWait = TimeUnit.SECONDS.toNanos(1);
    private final String id;
    private final String name;
    private final String description;
    private final String iconFile;
    private final List<ConfigurationCallback> callbacks;
    private final ReadWriteLock lock;
    private final Lock r, w;
    private final Condition updated;
    private final Map<String, Attribute> attributeToGetterMapping;
    private final Map<String, Attribute> attributeToSetterMapping;
    private final List<Method> allowedMethods;
    private final Class type;
    private final Object defaults;
    private final List<String> requiredIds;
    private final Map<FilterReference, Class<? extends TypeFilter>> defaultFilters;
    private ManagedPropertiesProvider provider;
    private boolean settingConfiguration = false;

    /**
     * Returns a ManagedProperties object with the defined defaults.
     *
     * @param <E> The type of the configuration interface to proxy.
     * @param type The type of configuration to create. This MUST be an
     * interface and the interface must be annotated with
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}.
     * @param defaults The default values to use if a configuration item is not
     * currently in the configuration set. The defaults must be an object of a
     * type that implements the type denoted by @see I.
     * @param defaultFiltersList An optional list of filters to use for default
     * filtering. This list will be used if an attributes input type does snot
     * match the output type defined by its output type defined by its methods
     * returntype.
     * @throws InvalidTypeException If a method/configuration item mapping uses
     * an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses
     * an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an
     * ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any
     * restriction not defined in the other exceptions.
     */
    public <E> ManagedPropertiesController(Class<? super E> type, E defaults, List<Class<? extends TypeFilter>> defaultFiltersList) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
        PropertyDefinition typeDefinition = getDefinitionAnnotation(type);
        callbacks = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
        r = lock.readLock();
        w = lock.writeLock();
        updated = w.newCondition();
        attributeToGetterMapping = new HashMap<>();
        attributeToSetterMapping = new HashMap<>();
        requiredIds = new ArrayList<>();

        this.defaultFilters = getDefaultFilterMap(defaultFiltersList);

        for (Method classMethod : type.getMethods()) {
            Property methodAnnotation = getMethodAnnotation(classMethod);
            if (methodAnnotation != null) {
                if (classMethod.getParameterTypes().length > 0) {
                    throw new InvalidMethodException("Could not create handler for this method. Methods annotated with " + Property.class.getName()
                            + " must not take parameters");
                }
                Attribute methodDefinition = new Attribute(classMethod, defaultFilters);
                if (methodDefinition.getCardinalityDef().equals(Property.Cardinality.Required)) {
                    requiredIds.add(methodDefinition.getID());
                }
                logger.debug("Adding method to mapping: " + methodDefinition);
                attributeToGetterMapping.put(classMethod.getName(), methodDefinition);
                
                if (methodDefinition.getSetterName() != null) {
                    Method setterMethod = null;
                    try {
                        setterMethod = type.getDeclaredMethod(methodDefinition.getSetterName(), methodDefinition.getMethodReturnType());
                    } catch (NoSuchMethodException | SecurityException ex) {
                        //No setter method. Don't do anything.
                    }
                    if(setterMethod == null){
                        try {
                        setterMethod = type.getDeclaredMethod(methodDefinition.getSetterName(), methodDefinition.getInputType());
                    } catch (NoSuchMethodException | SecurityException ex) {
                        //No setter method. Don't do anything.
                    }
                    }
                    
                    if (setterMethod != null) {
                        logger.debug("Adding setter method to mapping: " + methodDefinition.getSetterName());
                        attributeToSetterMapping.put(methodDefinition.getSetterName(), methodDefinition);
                    }
                }
            }
        }

        ensureUniqueIDs(attributeToGetterMapping.values());

        allowedMethods = new ArrayList<>();

        allowedMethods.addAll(Arrays.asList(PropertyActions.class.getDeclaredMethods()));
        allowedMethods.addAll(Arrays.asList(PropertyConfig.class.getDeclaredMethods()));
        allowedMethods.addAll(Arrays.asList(ConfigurationTarget.class.getDeclaredMethods()));
        allowedMethods.addAll(Arrays.asList(ConfigurationCallbackHandler.class.getDeclaredMethods()));
        allowedMethods.addAll(Arrays.asList(Object.class.getDeclaredMethods()));
        this.type = type;
        this.defaults = defaults;
        id = getDefinitionID(type);
        name = getDefinitionName(type);
        description = typeDefinition.description();
        iconFile = typeDefinition.iconFile();
    }

    /**
     * The same as the other constructor, but does not set defaults.
     *
     * @see #ManagedPropertiesController(Class, Object, List)
     * @param type The type of configuration to create. This MUST be an
     * interface and the interface must be annotated with
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}.
     * @param defaultFiltersList An optional list of filters to use for default
     * filtering. This list will be used if an attributes input type does snot
     * match the output type defined by its output type defined by its methods
     * returntype.
     * @throws InvalidTypeException If a method/configuration item mapping uses
     * an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses
     * an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an
     * ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any
     * restriction not defined in the other exceptions.
     */
    public ManagedPropertiesController(Class type, List<Class<? extends TypeFilter>> defaultFiltersList) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
        this(type, null, defaultFiltersList);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InvalidTypeException, TypeFilterException, InvocationException, UnknownValueException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException, ParsingException {
        String methodName = method.getName();
        Attribute getterPropertyDefinition = attributeToGetterMapping.get(methodName);
        Attribute setterPropertyDefinition = attributeToSetterMapping.get(methodName);
        if (method.isAnnotationPresent(Property.class)) {             
            if (methodName.equals(getterPropertyDefinition.getGetterName())) {
                return getConfigItem(method, getterPropertyDefinition);
            }
        }else if (setterPropertyDefinition != null && setterPropertyDefinition.getSetterName().equals(methodName) && args.length == 1){
            setItem(setterPropertyDefinition, args[0]);
            return null;
        }else if (allowedMethods.contains(method)) {
            try {
                return method.invoke(this, args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new InvocationException("Could not execute method. Execution of method " + method + " failed", ex);
            }
        }

        throw new UnsupportedOperationException("The method " + method + " was not recognized and was not annotated with the annotation " + Property.class.getName() + " allowed methods: " + allowedMethods);
    }
    
    public void setItem(Attribute propertyDefinition, Object item) throws ParsingException{
        if(item == null){
            return;
        }
        Class methodArgumentClass = item.getClass();
                if(propertyDefinition.getMethodReturnType().isAssignableFrom(methodArgumentClass)){
                    setFilteredItem(propertyDefinition, item);
                }else if(propertyDefinition.getInputType().isAssignableFrom(methodArgumentClass)){
                    setUnfilteredItem(propertyDefinition, item);
                }else if(List.class.isAssignableFrom(methodArgumentClass)){
                    setListOfItems(propertyDefinition, (List)item);
                }else{
                    throw new ParsingException("Could ot assign "+propertyDefinition.getName()+"["+propertyDefinition.getID()+"] with "+item+". Item was an unknown type. Expected "
                            + propertyDefinition.getInputType()+" or "+propertyDefinition.getMethodReturnType());
                }
    }
    
    private void setListOfItems(Attribute attribute, List<Object> items) throws ParsingException{
        List<Object> toSet = new ArrayList<>();
        for(Object item : items){
            if(attribute.getMethodReturnType().isAssignableFrom(item.getClass())){
                toSet.add(parseToConfig(attribute, item));
            }else{
                toSet.add(item);
            }
        }
        setItem(attribute.getID(), toSet);
    }
    
    private void setFilteredItem(Attribute attribute, Object item) throws ParsingException{
        parseToConfig(attribute, item);/*Test that we can actually parse the object before saving it, but save the unparsed item.*/

        setItem(attribute.getID(), item);
    }
    
    private void setUnfilteredItem(Attribute attribute, Object item) throws ParsingException{
        Object toSave = item;
        if(attribute.getFilter() != null){
            toSave = parseObject(item, attribute);
        }


        setItem(attribute.getID(), toSave);
    }

    private void setItem(String key, Object item) {
        w.lock();/*<- This is not a hanging lock. It unlocks in commitProperties*/
        try {

            config.put(key, item);

            if (settingConfiguration) {
                w.unlock();
            }
        } finally {
            settingConfiguration = true;
        }
    }

    @Override
    public void commitProperties() throws InvocationException {
        w.lock();
        try{
            if (!settingConfiguration) {
                throw new InvocationException("Could not commit properties, not currently setting configuration");
            }
        }finally{
            w.unlock();
        }
        
        try {
            
            
            HashMap<String, Object> parsedConfig = new HashMap<>();
            for(Attribute attribute : getAttributes()){
                Object value = getConfigItem(attribute.getID());
                if(value != null){
                    if(attribute.getFilter() != null){
                        value = parseToConfig(attribute, value);
                    }
                    
                    parsedConfig.put(attribute.getID(), value);
                }
            }

            provider.persistConfiguration(parsedConfig);
            
        } catch (ParsingException ex) {
            logger.error("Could not parse configuration", ex);
        }finally{
            settingConfiguration = false;
            w.unlock();/*<- This is not a hanging lock. It locks in setItem*/
        }

    }

    private Object getConfigItem(Method method, Attribute propertyDefinition) throws InvocationException, InvalidTypeException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {
        Object returnValue = getConfigItem(propertyDefinition.getID());
        if (returnValue == null) {
            returnValue = getDefaultItem(method);
            if (returnValue == null && propertyDefinition.isRecentlyUpdated()) {
                w.lock();
                try {
                    logger.debug("Value was null. Awaiting update");
                    updated.awaitNanos(nanosToWait);
                    returnValue = getConfigItem(propertyDefinition.getID());
                    logger.debug("Waited for value: " + returnValue);
                    propertyDefinition.setRecentlyUpdated(false);
                } finally {
                    w.unlock();
                }

            }
            if (returnValue == null) {
                throw new UnknownValueException("Could not return the value for method " + method.getName() + ". The value did not exist in the config set.");
            }
        }
        if (!method.getReturnType().isAssignableFrom(returnValue.getClass())) {
            throw new InvalidTypeException("Could not return the value for method " + method.getName() + ". The value " + returnValue + " had Type " + returnValue.getClass() + " expected " + method.getReturnType());
        }
        return returnValue;
    }

    public Object getConfigItem(String id) {
        r.lock();
        try {
            return config.get(id);
        } finally {
            r.unlock();
        }
    }

    private Object getDefaultItem(Method method) throws InvocationException, InvalidTypeException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (defaults == null) {
            //throw new UnknownValueException("Could not return the value for method " + method.getName() + " the value did not exist in the config set "
            //	    + "and no defaults were found");
            return null;
        }
        if (!type.isAssignableFrom(defaults.getClass())) {
            //This SHOULD not be possible, given the generic constructor. But still. Better safe than sorry.
            throw new InvocationException("Could not get defaults. The defaults " + defaults + " are not of the expected type " + type);
        }
        Method defaultValueProvider = findDefaultMethod(defaults.getClass(), method);
        return defaultValueProvider.invoke(defaults, new Object[0]);

    }

    private Method findDefaultMethod(Class clazz, Method method) throws UnknownValueException {
        try {
            return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ex) { //The method was not declared here. Go to the superclass and retry.
            if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
                return findDefaultMethod(clazz.getSuperclass(), method);
            } else {
                throw new UnknownValueException("Could not return the value for method " + method.getName() + " the value did not exist in the config set "
                        + "and no matching method existed in the defaults");
            }
        }
    }

    /**
     * The updated method is called in order to change the configuration bound
     * to this class. When called, the method will parse the Map of properties
     * and parse the data for each Method/Configuration item. For each
     * Configuration Item, the type of the object is checked against the return
     * types, the filters are run, and the data parsed. The result is that it is
     * the final, parsed and validated versions of the configuration that is
     * actually returned. The original properties are never stored in this
     * object.
     *
     * @param properties The properties that are sent from the configuration
     * source
     * @return The configuration keys and values that could not be matched to a
     * configuration item
     * @throws ParsingException If the data is invalid, or a filter fails.
     */
    @Override
    public Map<String, Object> updateConfig(Map<String, Object> properties) throws ParsingException {
        if (logger.isInfoEnabled()) {
            boolean printKeys = logger.isDebugEnabled();
            Set<String> configHiddenKeys = getHiddenKeys();
            StringBuilder builder = new StringBuilder();
            String delim = "";
            for (String key : properties.keySet()) {
                builder.append(delim).append(key).append("[").append(properties.get(key).getClass().getSimpleName()).append("]");
                if (printKeys) {
                    if (configHiddenKeys.contains(key)) {
                        builder.append("=").append("******");
                    } else {
                        builder.append("=").append(properties.get(key));
                    }
                }
                delim = ", ";
            }
            logger.info("Attempting to update properties on " + name + "[" + id + "] with " + builder.toString());
        }

        Map<String, Object> unknownConfigs = new HashMap<>();
        if (properties == null) {
            throw new ParsingException("Could not update configuration. Map was null");
        }

        w.lock();
        try {
        TreeSet<String> required = new TreeSet<String>();
        required.addAll(requiredIds);
        Map<String, Object> newprops = new HashMap<>();
        
            Set<String> keys = properties.keySet();
            for (String key : keys) {

                Attribute definition = null;
                for (Attribute possibleDefinition : attributeToGetterMapping.values()) {
                    if (possibleDefinition.getID().equals(key)) {
                        definition = possibleDefinition;
                        break;
                    }
                }
                if (definition == null) {
                    logger.debug(key, "Could not load property. The property " + key + " is not known to this configuration. Supported methods: " + attributeToGetterMapping.keySet());
                    unknownConfigs.put(key, properties.get(key));
                }
                Object configValue = null;
                switch (definition.cardinalityDef) {
                    case Optional:
                        configValue = properties.get(key);
                        configValue = parseObject(configValue, definition);
                        break;
                    case Required:
                        required.remove(definition.getID());
                        configValue = properties.get(key);
                        configValue = parseObject(configValue, definition);
                        break;
                    case List:
                        List<Object> valueAsList = retrieveList(key, properties.get(key));
                        List<Object> outputList = new ArrayList<>();
                        for (Object value : valueAsList) {
                            Object listValue = parseObject(value, definition);
                            outputList.add(listValue);
                        }
                        configValue = outputList;
                        break;
                }
                newprops.put(key, configValue);

            }
            if (!required.isEmpty()) {
                throw new ParsingException(required.pollFirst(), "Could not update configuration. Missing required fields: " + new ArrayList<>(required));
            }
            config = newprops;
            for (Attribute attribute : attributeToGetterMapping.values()) {
                attribute.setRecentlyUpdated(true);
            }
            for (ConfigurationCallback callback : callbacks) {
                callback.configurationUpdated(properties);
            }
        } finally {
            logger.debug("Finished updating configuration. Signalling all");
            updated.signalAll();
            w.unlock();

        }

        logger.info("updated configuration\n" + this);
        return unknownConfigs;
    }
    
    

    public Object parseObject(Object configurationValue, Attribute definition) throws ParsingException {
        if (!definition.getInputType().isAssignableFrom(configurationValue.getClass())) {
            throw new ParsingException(definition.getID(), "Could not assign " + configurationValue.getClass() + " to " + definition.getInputType() + ". Value of key "+definition.getID()+" must be of type "+definition.getInputType());
        }
        
        
        Object toReturn = configurationValue;
        
        
        if (definition.getFilter() != null) {
            toReturn = filterObject(definition.getID(), toReturn, definition.getFilter(), definition);
        }

        return toReturn;

    }

    

    public Object parseToConfig(Attribute definition, Object typedObject) throws ParsingException {
        if (definition == null) {
            throw new ParsingException(definition.getID(), "Could not load property. The property " + definition.getID() + " is not known to this configuration. Supported methods: " + attributeToGetterMapping.keySet());
        }

        Object toReturn = typedObject;
        if (definition.getFilter() != null) {
            toReturn = filterReversedObject(definition.getID(), toReturn, definition.getFilter(), definition);
        }

        return toReturn;
    }

    

    private void ensureUniqueIDs(Collection<Attribute> attributeDefinitions) throws DoubleIDException {
        TreeSet<Attribute> adSet = new TreeSet<>(new Comparator<Attribute>() {

            @Override
            public int compare(Attribute o1, Attribute o2) {
                return o1.getID().compareTo(o2.getID());
            }
        });

        for (Attribute attributeDefinition : attributeDefinitions) {
            if (adSet.contains(attributeDefinition)) {
                throw new DoubleIDException("Could not add the attribute " + attributeDefinition + " with id " + attributeDefinition + ". ID already used");
            }
            adSet.add(attributeDefinition);
        }

    }
    
    private Object retrieveObjectFromCollection(String key, Object configItemObject) throws ParsingException {
        if (Collection.class.isAssignableFrom(configItemObject.getClass())) {
            List configItemList = new ArrayList<>((Collection) configItemObject);
            if (configItemList.isEmpty()) {
                return null;
            } else if (configItemList.size() == 1) {
                return configItemList.get(0);
            } else {
                throw new ParsingException(key, "The optional value " + key + " had more than one item assigned to it: " + configItemList);
            }
        } else {
            return configItemObject;
        }

    }

    private List retrieveList(String key, Object configItemObject) throws ParsingException {
        if (!List.class.isAssignableFrom(configItemObject.getClass())) {
            throw new ParsingException(key, "This value should be a List: " + configItemObject);
        }
        List configItemList = (List) configItemObject;

        return configItemList;
    }

    private Object filterObject(String key, Object input, Class<? extends TypeFilter> filterType, Attribute attribute) throws ParsingException {
        if (!attribute.getInputType().isAssignableFrom(input.getClass())) {
            throw new ParsingException(key, "Could not filter this object. The input type was incorrect. Expected " + attribute.getInputType() + " found " + input.getClass());
        }
        try {
            TypeFilter filterinstance = filterType.newInstance();
            Object filterValue = filterinstance.parse(input);
            return filterValue;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new ParsingException(key, "Could not load properties. Could not instantiate filter.", ex);
        } catch (TypeFilterException ex) {
            throw new ParsingException(key, "Could not load properties. Could not filter value.", ex);
        }
    }
    
        private Object filterReversedObject(String key, Object input, Class<? extends TypeFilter> filterType, Attribute attribute) throws ParsingException {
        if (!attribute.getMethodReturnType().isAssignableFrom(input.getClass())) {
            throw new ParsingException(key, "Could not filter this object. The input type was incorrect. Expected " + attribute.getMethodReturnType() + " found " + input.getClass());
        }
        try {
            TypeFilter filterinstance = filterType.newInstance();
            Object filterValue = filterinstance.revert(input);
            return filterValue;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new ParsingException(key, "Could not load properties. Could not instantiate filter.", ex);
        } catch (TypeFilterException ex) {
            throw new ParsingException(key, "Could not load properties. Could not filter value.", ex);
        }
    }

    @Override
    public void addConfigurationCallback(ConfigurationCallback callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }

    @Override
    public void removeConfigurationCallback(ConfigurationCallback callback) {
        synchronized (callbacks) {
            callbacks.remove(callback);
        }
    }

    @Override
    public List<ConfigurationCallback> getConfigurationCallbacks() {
        synchronized (callbacks) {
            return new ArrayList<>(callbacks);
        }
    }

    @Override
    public Lock lockPropertiesUpdate() {
        r.lock();
        return r;
    }

    @Override
    public void unregisterProperties() throws Exception {
        logger.info("Unregistering properties for " + id);
        provider.stop();
    }

    @Override
    public void setPropertyWriteDelay(int delay, TimeUnit unit) {
        nanosToWait = unit.toNanos(delay);
    }

    protected final Map<FilterReference, Class<? extends TypeFilter>> getDefaultFilterMap(List<Class<? extends TypeFilter>> filters) throws TypeFilterException {
        Map<FilterReference, Class<? extends TypeFilter>> filterMap = new HashMap<>();
        for (Class<? extends TypeFilter> filter : filters) {
            FilterReference reference = getReference(filter);
            logger.debug("Checking defaults reference: " + reference);
            if (filterMap.containsKey(reference)) {
                throw new TypeFilterException("Could not create defaults. Only one filter must have the same combination of input and output types");
            }
            filterMap.put(reference, filter);
        }
        return filterMap;
    }

    protected final FilterReference getReference(Class<? extends TypeFilter> filterClass) throws TypeFilterException {
        Method[] methods = filterClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("parse") && method.getParameterTypes().length == 1 && !method.isBridge()) {
                Class inputType = method.getParameterTypes()[0];
                Class outputType = method.getReturnType();
                return new FilterReference(inputType, outputType);
            }
        }
        throw new TypeFilterException("Could not find a parse method on this typefilter: " + filterClass);
    }

    protected final Set<String> getHiddenKeys() {
        Set<String> configHiddenKeys = new HashSet<>();
        for (Attribute attribute : attributeToGetterMapping.values()) {
            if (attribute.isHidden()) {
                configHiddenKeys.add(attribute.getID());
            }
        }
        return configHiddenKeys;
    }

    @Override
    public String toString() {
        Set<String> configHiddenKeys = getHiddenKeys();

        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        builder.append("id", id);
        builder.append("name", name);
        builder.append("description", description);
        r.lock();
        try {
            for (String key : config.keySet()) {
                if (configHiddenKeys.contains(key)) {
                    builder.append(key, "******");
                } else {
                    builder.append(key, config.get(key));
                }
            }
        } finally {
            r.unlock();
        }
        return builder.build();
    }

    @Override
    public Class getConfigurationType() {
        return type;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getIconFile() {
        return iconFile;
    }

    @Override
    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributeToGetterMapping.values());
    }

    public Attribute getAttributeByName(String methodName) {
        return attributeToGetterMapping.get(methodName);
    }

    public ManagedPropertiesProvider getProvider() {
        return provider;
    }

    public void setProvider(ManagedPropertiesProvider provider) {
        this.provider = provider;
    }

    public static final Property getMethodAnnotation(Method toScan) throws InvalidMethodException {
        if (toScan == null) {
            throw new InvalidMethodException("Could not find property for this method. toScan was null");
        }

        List<Property> annotations = getAnnotations(toScan);

        if (annotations.isEmpty()) {
            return null;
        }
        if (annotations.size() > 1) {
            throw new InvalidMethodException("Could not get method annotation for " + toScan.getName() + ". More than one instance of " + Property.class.getSimpleName() + " was found in the heirachy for this method");
        }

        return annotations.get(0);

    }

    private static List<Property> getAnnotations(Method toScan) {
        List<Property> annotations = new ArrayList<>();
        if (toScan.isAnnotationPresent(Property.class)) {
            annotations.add(toScan.getAnnotation(Property.class));
        }

        for (Class superInterface : toScan.getDeclaringClass().getInterfaces()) {
            try {
                Method method = superInterface.getDeclaredMethod(toScan.getName(), toScan.getParameterTypes());
                annotations.addAll(getAnnotations(method));
            } catch (NoSuchMethodException | SecurityException ex) {
                //There was no method with that signature in this interface. Stop looking!
            }
        }
        return annotations;
    }

    public static List<PropertyDefinition> getDefinition(Class<?> toScan) {
        List<PropertyDefinition> definitions = new ArrayList<>();
        if (toScan.isAnnotationPresent(PropertyDefinition.class)) {
            definitions.add(toScan.getAnnotation(PropertyDefinition.class));
        }
        for (Class<?> parent : toScan.getInterfaces()) {
            definitions.addAll(getDefinition(parent));
        }
        return definitions;
    }

    public static String getDefinitionID(Class<?> type) throws InvalidTypeException {
        PropertyDefinition typeDefinition = getDefinitionAnnotation(type);
        if (typeDefinition.id().isEmpty()) {
            return type.getCanonicalName();
        } else {
            return typeDefinition.id();
        }
    }

    public static String getDefinitionName(Class<?> type) throws InvalidTypeException {
        PropertyDefinition typeDefinition = getDefinitionAnnotation(type);
        if (typeDefinition.name().isEmpty()) {
            return type.getSimpleName();
        } else {
            return typeDefinition.name();
        }
    }

    public static final PropertyDefinition getDefinitionAnnotation(Class<?> type) throws InvalidTypeException {
        if (type == null) {
            throw new InvalidTypeException("Could not build OCD. Type was null");
        }

        List<PropertyDefinition> definitions = getDefinition(type);

        if (definitions.isEmpty()) {
            throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". Type did not contain the annotation " + PropertyDefinition.class.getName());
        }

        /*	if (typeDefinition.id().isEmpty()) {
	    throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". ID was not set on " + PropertyDefinition.class.getSimpleName());
	}

	if (typeDefinition.name().isEmpty()) {
	    throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". Name was not set on " + PropertyDefinition.class.getSimpleName());
	}
         */
        if (definitions.size() > 1) {
            throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". More than one instance of " + PropertyDefinition.class.getSimpleName() + " was found in the heirachy");
        }
        return definitions.get(0);
    }

}
