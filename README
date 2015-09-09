# ManagedProperties
This project aims to simplify the process of making an OSGi bundle interact with the [Felix Configuration Admin](http://felix.apache.org/documentation/subprojects/apache-felix-config-admin.html)
 and [MetaType](http://felix.apache.org/documentation/subprojects/apache-felix-metatype-service.html).
 The ManagedProperties bundle hosts a Service called register. In order to use ManagedProperties to register your configuration, annotate an interface
 and register it with the service.
 
 
## Simple usage
 At its simplest, the service takes an annotated interface, for example:
```
@PropertyDefinition(id = "SomeID", name = "Some better name")
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
and registers it at the service:

```
	ServiceTracker tracker = new ServiceTracker(context, ManagedPropertiesService.class, null);
  tracker.open();
	ManagedPropertiesService service = tracker.getService();
	BundleProperties props = service.register(BundleProperties.class, null);
	tracker.close();
```
ManagedProperties then parses the information, creates the ObjectClassDefinition and AttributeDefinitions and registers the configuration
as both a ManagedService and MetaTypeProvider.
The `Property` annotation can take a number of options, but if none are given, as in the example above, ManagedProperties will use the method
name (removing the "get" if present) as the name and ID, and the returnType as the configuration object type.
The interface definition and register calls is all you need to register a configuration with accompanying MetaType information.

## Defaults
In case of a project where some (or all) of the configuration items are not available, the ManagedProperties service can take an object with
defaults, along with the interface type.
In order to use defaults, simply implement the interface used for the properties, and pass that along with the register method.
```
	ServiceTracker tracker = new ServiceTracker(context, ManagedPropertiesService.class, null);
  tracker.open();
	ManagedPropertiesService service = tracker.getService();
	BundleProperties props = service.register(BundleProperties.class, new BundlePropertiesDefaults());
	tracker.close();
```
Where BundlePropertiesDefaults is an implementation of the BundleProperties interface.

This is actually a fairly powerful defaults mechanism, as it just uses an implemented interface. The implementationclass could contain logic, take
defaults from a database or shared service, or whatever the need might be.

## TypeFilters
The Felix Configuration Admin takes only some primitive types as valid parameters. This is OK for some usages, but actually pretty boring.
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
Admin as a String) will be run through the FileFilter to produce a File. If the filename is not correct with respect to the OS,
a ConfigurationException will be thrown.

TypeFilters are used to convert simple ConfigAdmin types to other types, as well as giving the oppotunity to add validation logic.

## Callbacks
In case that an application needs to be made aware of configuration changes, and application can register a CallBack with the
returned ManagedProperties object.
```
ServiceTracker tracker = new ServiceTracker(context, ManagedPropertiesService.class, null);
  tracker.open();
	ManagedPropertiesService service = tracker.getService();
	BundleProperties props = service.register(BundleProperties.class, new BundlePropertiesDefaults());
	tracker.close();
	ConfigurationCallback callback = new ConfigurationCallbackImpl();
((ConfigurationCallbackHandler)props).addConfigurationCallback(callback);
```
The callback will then have its configurationUpdated method called every time the configuration is updated from the Configuration Admin.

## Registration mechanics
When an annotated Interface is registered with ManagedProperties, what happens behind the scenes is the following:
1. An object is created in order to track the registrations.
2. An ObjectClassDefinition is created. 
3. The types of the methods and filters are checked to assure that
  * The input types are valid ConfigAdmin types
  * The output matches the methods return type
  * In case of a TypeFilter being present, that the input and output matches the input types and returntype of the method.
4. A java.util.proxy is created in order to wrap the tracker object.
5. The proxy is returned.