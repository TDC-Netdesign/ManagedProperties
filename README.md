# ManagedProperties
This project aims to simplify the process of making an OSGi bundle interact with the [Felix Configuration Admin](http://felix.apache.org/documentation/subprojects/apache-felix-config-admin.html)
 and [MetaType](http://felix.apache.org/documentation/subprojects/apache-felix-metatype-service.html).
 The ManagedProperties services provides a wat to bind a Configuration Admin configuration to an Interface representing the configuration items therein.
 In order to use ManagedProperties to register your configuration, annotate an interface and provide it to the Service. An object is returned implementing that interface, which can be called
 to retrieve configuration from the configuration source (Configuration Admin)

## Requirements
A quick rundown of what is needed to use this bundle.

### Required Container
The ManagedProperties bundle should work on any Felix container, but is currently only tested on Karaf.
It requires that the Felix Configuration Admin and MetaType are active in order to function.

### Required Bundles
The only bundles required for the service to function is dk.netdesign:managedproperties-service:LATEST as well as its sole dependency org.apache.commons:commons-lang3:[3.4). The other bundles are there for examples and tests, and are likely
not relevant or interesting. 

### Getting the Bundles
The bundles can be found on the Maven Central Repository
 
 
## Simple usage
ManagedProperties will register a configuration as an interface, which defines the configuration items to use. When an interface is registered, a proxy responsible for bridging the
configuration is returned.

When an interface is returned, its getter methods can be used to retrieve the configuration from the backing configuration source. If the configuration updates, the interface implementation
will update transparently from the user.


###Configuration Interface
 At its simplest, the service takes an annotated interface, for example:
```
@PropertyDefinition(id = "SomeID", name = "A humanfriendly name")
public interface BundleProperties{

	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;

	@Property
	public Integer getInteger() throws InvalidTypeException, TypeFilterException;

	@Property
	public Long getLong() throws InvalidTypeException, TypeFilterException;

	@Property
	public Short getShort() throws InvalidTypeException, TypeFilterException;

	@Property
	public Character getCharacter() throws InvalidTypeException, TypeFilterException;

	@Property
	public Byte getByte() throws InvalidTypeException, TypeFilterException;
}
```
and registers it with the factory:

###Configuration registration
 In order to register an Interface to a configuration, there are two options, where one i currently experimental.
####Service registration
```
	ManagedPropertiesService managedPropertiesService;
    managedPropertiesService = context.getService(serviceRef);
	BundleProperties props = managedPropertiesService.register(BundleProperties.class, context);
```

####Static factory registration [EXPERIMENTAL]
```
	BundleProperties props = ManagedPropertiesServiceFactory.registerProperties(BundleProperties.class, context);
```
ManagedProperties then parses the information, creates the ObjectClassDefinition and AttributeDefinitions and registers the configuration
as both a ManagedService and MetaTypeProvider.
The `Property` annotation can take a number of options, but if none are given, as in the example above, ManagedProperties will use the method
name (removing the "get" if present) as the name and ID, and the returnType as the configuration object type.
The interface definition and register calls is all you need to register a configuration with accompanying MetaType information.
Note that you do not strictly **need** to add the exceptions in the ```throws``` clause of the interface. Omitting this will cause the the exceptions
to be thrown wrapped in a ```java.lang.reflect.UndeclaredThrowableException```, which is a RuntimeException.

## Defaults
In case of a project where some (or all) of the configuration items are not available, the ManagedProperties service can take an object with
defaults, along with the interface type.
In order to use defaults, simply implement the interface used for the properties, and pass that along with the register method.
```
	BundleProperties props = managedPropertiesService.register(BundleProperties.class, new BundlePropertiesDefaults(), context);
```
Where BundlePropertiesDefaults is an implementation of the BundleProperties(The configuration) interface.

This is actually a fairly powerful defaults mechanism, as it just uses an implemented interface. The implementationclass could contain logic, take
defaults from a database or shared service, or whatever the need might be.

## TypeFilters
The Felix Configuration Admin takes only some primitive types as valid parameters. This is OK for some usages, but does require the implementor to do a fair amount of work.
Sometimes, an application might need some more advanced types or maybe even logic. In order to meet this requirement, ManagedProperties
introduces a feature called TypeFilters. A TypeFilter is an abstract class that implements a method `public abstract Object parse(I input) throws TypeFilterException`
which takes an input, in order to produce an output object.
In order to use a TypeFilter, specify the filter in the `Property`.
```
@PropertyDefinition(id = "SomeID", name = "Some better name")
public interface BundleProperties{
  @Property(type = String.class, typeMapper = FileFilter.class)
	public File getFile() throws InvalidTypeException, TypeFilterException;
}
```
A few things happens here. The @Property defines a Type, and typeMapper, and the method return type is File.
* `type` instructs ManagedProperties to register the configuration item as a String in the Configuration Admin.
* `typeMapper` instructs ManagedProperties to use the FileFilter to parse the incomming String to a File(The method return type)


When a new configuration is received from the Configuration Admin Service, the File property(Which will be stored in the Config 
Admin as a String) will be run through the FileFilter to produce a File. If the filename is not correctly formed with respect to the OS,
a ConfigurationException will be thrown.

TypeFilters are used to convert simple ConfigAdmin types to other types, as well as giving the opportunity to add validation logic.

###Default filters
Version 1.0 of the ManagedProperties service introduces Default Filters. Default filters are provided by registering a service of the type 
`dk.netdesign.common.osgi.config.service.DefaultFilterProvider`, which must return a list of TypeFilter implementations. A Filter is defined 
by the return type and type of the input parameter of its parse method. Only one default filter with any one combination of input and output type
must be defined.

When a property `@Property` is defined with a `type` definition that the config admin does not supply, a default filter will attempt to fill the void
but only if the input and output matches the `@Property` returntype and type exactly.
For example:
@Property(type = String.class) public Integer getInteger() - will attempt to find a default filter with String input and Integer output types.

If the configuration source(In this case the configuration admin) is not able to return the return-type of the method, a default filter will attempt to span the gap.
@Property public Integer getInteger() will attempt to find a default filter with String input and Integer output types if the ConfigAdmin will only return Strings.


## Callbacks
In case that an application needs to be made aware of configuration changes, and application can register a CallBack with the
returned ManagedProperties object.
```
	BundleProperties props = ManagedPropertiesFactory.register(BundleProperties.class, new BundlePropertiesDefaults());
	ConfigurationCallback callback = new ConfigurationCallbackImpl();
    PropertyAccess.callbacks(type).addConfigurationCallback(callback);
```
The callback will then have its configurationUpdated method called every time the configuration is updated from the Configuration Admin.

## Registration mechanics
When an annotated Interface is registered with ManagedProperties, what happens behind the scenes is the following:

1. An object is created in order to track the registrations.
2. An ObjectClassDefinition is created. 
3. Default filters are parsed to fill any gaps.
4. The types of the methods and filters are checked to assure that
  * The input types are valid ConfigAdmin types
  * The output matches the methods return type
  * In case of a TypeFilter being present, that the input and output matches the input types and returntype of the method.
5. A java.util.proxy is created in order to wrap the tracker object.
6. The proxy is returned.

## Deregistration
When a configuration object is done being used, it must be deregistered. That is done by the following call:
`PropertyAccess.actions(type).unregisterProperties();`

## Waiting time
As default behaviour, the configuration object will wait 5 seconds for configuration, if it is not present when queried. This can be changed by a call to
`PropertyAccess.configuration(type).setPropertyWriteDelay(1, TimeUnit.SECOND);`

## Locking
If a combination of configuration objects must be used in conjuction (Such as a username and password) the property object can be temporarily locked. The configuration will not update
while the object is locked.

Example:
`Lock lock = PropertyAccess.actions(type).lockPropertiesUpdate();`
and to unlock
`lock.unlock()`
