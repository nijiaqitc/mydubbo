package com.njq.mydubbo.config.api;

import com.njq.mydubbo.common.URL;
import com.njq.mydubbo.common.URLBuilder;
import com.njq.mydubbo.common.Version;
import com.njq.mydubbo.common.config.Environment;
import com.njq.mydubbo.common.constants.CommonConstants;
import com.njq.mydubbo.common.constants.RegistryConstants;
import com.njq.mydubbo.common.utils.Assert;
import com.njq.mydubbo.common.utils.CollectionUtils;
import com.njq.mydubbo.common.utils.ConfigUtils;
import com.njq.mydubbo.common.utils.ReflectUtils;
import com.njq.mydubbo.common.utils.StringUtils;
import com.njq.mydubbo.common.utils.UrlUtils;
import com.njq.mydubbo.config.api.context.ConfigManager;
import com.njq.mydubbo.config.api.support.Parameter;
import com.njq.mydubbo.registry.api.RegistryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.njq.mydubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static com.njq.mydubbo.registry.api.Constants.REGISTER_KEY;
import static com.njq.mydubbo.registry.api.Constants.SUBSCRIBE_KEY;
import static com.njq.mydubbo.remoting.api.Constants.DUBBO_VERSION_KEY;

/**
 * @author: nijiaqi
 * @date: 2019/6/10
 */
public abstract class AbstractInterfaceConfig extends AbstractMethodConfig {
    /**
     * Local impl class name for the service interface
     */
    protected String local;

    /**
     * Local stub class name for the service interface
     */
    protected String stub;

    /**
     * Service monitor
     */
    protected MonitorConfig monitor;

    /**
     * Strategies for generating dynamic agents，there are two strategies can be choosed: jdk and javassist
     */
    protected String proxy;

    /**
     * Cluster type
     */
    protected String cluster;

    /**
     * if there are more than one, you can use commas to separate them
     */
    protected String filter;

    /**
     * The Listener when the provider side exposes a service or the customer side references a remote service used
     * if there are more than one, you can use commas to separate them
     */
    protected String listener;

    /**
     * The owner of the service providers
     */
    protected String owner;

    /**
     * Connection limits, 0 means shared connection, otherwise it defines the connections delegated to the current service
     */
    protected Integer connections;

    /**
     * The layer of service providers
     */
    protected String layer;

    /**
     * The application info
     */
    protected ApplicationConfig application;

    /**
     * The module info
     */
    protected ModuleConfig module;

    /**
     * Registry centers
     */
    protected List<RegistryConfig> registries;

    protected String registryIds;

    // connection events
    protected String onconnect;

    /**
     * Disconnection events
     */
    protected String ondisconnect;

    /**
     * The metrics configuration
     */
    protected MetricsConfig metrics;
    protected MetadataReportConfig metadataReportConfig;

    protected ConfigCenterConfig configCenter;

    // callback limits
    private Integer callbacks;
    // the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
    private String scope;

    protected String tag;

    /**
     * Check whether the registry config is exists, and then conversion it to {@link RegistryConfig}
     */
    protected void checkRegistry() {
        loadRegistriesFromBackwardConfig();

        convertRegistryIdsToRegistries();

        for (RegistryConfig registryConfig : registries) {
            if (!registryConfig.isValid()) {
                throw new IllegalStateException("No registry config found or it's not a valid config! " +
                        "The registry config is: " + registryConfig);
            }
        }

        useRegistryForConfigIfNecessary();
    }

    @SuppressWarnings("deprecation")
    protected void checkApplication() {
        // for backward compatibility
        createApplicationIfAbsent();

        if (!application.isValid()) {
            throw new IllegalStateException("No application config found or it's not a valid config! " +
                    "Please add <dubbo:application name=\"...\" /> to your spring config.");
        }


    }

    protected void checkMonitor() {
        createMonitorIfAbsent();
        if (!monitor.isValid()) {

        }
    }

    private void createMonitorIfAbsent() {
        if (this.monitor != null) {
            return;
        }
        ConfigManager configManager = ConfigManager.getInstance();
        setMonitor(
                configManager
                        .getMonitor()
                        .orElseGet(() -> {
                            MonitorConfig monitorConfig = new MonitorConfig();
                            monitorConfig.refresh();
                            return monitorConfig;
                        })
        );
    }

    protected void checkMetadataReport() {

    }


    void startConfigCenter() {
        if (configCenter == null) {
            ConfigManager.getInstance().getConfigCenter().ifPresent(cc -> this.configCenter = cc);
        }

        if (this.configCenter != null) {
            // TODO there may have duplicate refresh
            this.configCenter.refresh();
            prepareEnvironment();
        }
        ConfigManager.getInstance().refreshAll();
    }

    private void prepareEnvironment() {

    }


    /**
     * Load the registry and conversion it to {@link URL}, the priority order is: system property > dubbo registry config
     *
     * @param provider whether it is the provider side
     * @return
     */
    protected List<URL> loadRegistries(boolean provider) {
        List<URL> registryList = new ArrayList<URL>();
        registries = new ArrayList<>();
        RegistryConfig fig = new RegistryConfig();
        fig.setAddress("multicast://224.5.6.7:1234");
        fig.setPrefix("dubbo.registries.");
        fig.setId("com.njq.mydubbo.config.api.RegistryConfig");
        registries.add(fig);
        application = new ApplicationConfig();
        application.setPrefix("dubbo.application");
        application.setName("demo-provider");
        application.setId("abcc");

        if (CollectionUtils.isNotEmpty(registries)) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                if (StringUtils.isEmpty(address)) {
                    address = ANYHOST_VALUE;
                }
                if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    appendParameters(map, application);
                    appendParameters(map, config);
                    map.put("path", RegistryService.class.getName());
                    appendRuntimeParameters(map);
                    if (!map.containsKey(CommonConstants.PROTOCOL_KEY)) {
                        map.put(CommonConstants.PROTOCOL_KEY, CommonConstants.DUBBO_PROTOCOL);
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);

                    for (URL url : urls) {
                        url = URLBuilder.from(url)
                                .addParameter(RegistryConstants.REGISTRY_KEY, url.getProtocol())
                                .setProtocol(RegistryConstants.REGISTRY_PROTOCOL)
                                .build();
                        if ((provider && url.getParameter(REGISTER_KEY, true))
                                || (!provider && url.getParameter(SUBSCRIBE_KEY, true))) {
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return registryList;
    }

    /**
     * Load the monitor config from the system properties and conversation it to {@link URL}
     *
     * @param registryURL
     * @return
     */
    protected URL loadMonitor(URL registryURL) {

        return null;
    }

    static void appendRuntimeParameters(Map<String, String> map) {
        map.put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
        map.put(CommonConstants.RELEASE_KEY, Version.getVersion());
        map.put(CommonConstants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(CommonConstants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
    }

    private URL loadMetadataReporterURL() {
        return null;
    }


    /**
     * Check whether the remote service interface and the methods meet with Dubbo's requirements.it mainly check, if the
     * methods configured in the configuration file are included in the interface of remote service
     *
     * @param interfaceClass the interface of remote service
     * @param methods        the methods configured
     */
    protected void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
        // interface cannot be null
        Assert.notNull(interfaceClass, new IllegalStateException("interface not allow null!"));

        // to verify interfaceClass is an interface
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        // check if methods exist in the remote service interface
        if (CollectionUtils.isNotEmpty(methods)) {
            for (MethodConfig methodBean : methods) {
                methodBean.setService(interfaceClass.getName());
                methodBean.setServiceId(this.getId());
                methodBean.refresh();
                String methodName = methodBean.getName();
                if (StringUtils.isEmpty(methodName)) {
                    throw new IllegalStateException("<dubbo:method> name attribute is required! Please check: " +
                            "<dubbo:service interface=\"" + interfaceClass.getName() + "\" ... >" +
                            "<dubbo:method name=\"\" ... /></<dubbo:reference>");
                }

                boolean hasMethod = Arrays.stream(interfaceClass.getMethods()).anyMatch(method -> method.getName().equals(methodName));
                if (!hasMethod) {
                    throw new IllegalStateException("The interface " + interfaceClass.getName()
                            + " not found method " + methodName);
                }
            }
        }
    }

    /**
     * Legitimacy check and setup of local simulated operations. The operations can be a string with Simple operation or
     * a classname whose {@link Class} implements a particular function
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface that will be referenced
     */
    void checkMock(Class<?> interfaceClass) {

    }

    /**
     * Legitimacy check of stub, note that: the local will deprecated, and replace with <code>stub</code>
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface
     */
    void checkStubAndLocal(Class<?> interfaceClass) {
        if (ConfigUtils.isNotEmpty(local)) {
            Class<?> localClass = ConfigUtils.isDefault(local) ?
                    ReflectUtils.forName(interfaceClass.getName() + "Local") : ReflectUtils.forName(local);
            verify(interfaceClass, localClass);
        }
        if (ConfigUtils.isNotEmpty(stub)) {
            Class<?> localClass = ConfigUtils.isDefault(stub) ?
                    ReflectUtils.forName(interfaceClass.getName() + "Stub") : ReflectUtils.forName(stub);
            verify(interfaceClass, localClass);
        }
    }

    private void verify(Class<?> interfaceClass, Class<?> localClass) {
        if (!interfaceClass.isAssignableFrom(localClass)) {
            throw new IllegalStateException("The local implementation class " + localClass.getName() +
                    " not implement interface " + interfaceClass.getName());
        }

        try {
            //Check if the localClass a constructor with parameter who's type is interfaceClass
            ReflectUtils.findConstructor(localClass, interfaceClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() +
                    "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
        }
    }

    private void convertRegistryIdsToRegistries() {


    }

    private void loadRegistriesFromBackwardConfig() {
        // for backward compatibility
        // -Ddubbo.registry.address is now deprecated.
        if (registries == null || registries.isEmpty()) {
            String address = ConfigUtils.getProperty("dubbo.registry.address");
            if (address != null && address.length() > 0) {
                List<RegistryConfig> tmpRegistries = new ArrayList<RegistryConfig>();
                String[] as = address.split("\\s*[|]+\\s*");
                for (String a : as) {
                    RegistryConfig registryConfig = new RegistryConfig();
                    registryConfig.setAddress(a);
                    registryConfig.refresh();
                    tmpRegistries.add(registryConfig);
                }
                setRegistries(tmpRegistries);
            }
        }
    }

    /**
     * For compatibility purpose, use registry as the default config center if the registry protocol is zookeeper and
     * there's no config center specified explicitly.
     */
    private void useRegistryForConfigIfNecessary() {
        registries.stream().filter(RegistryConfig::isZookeeperProtocol).findFirst().ifPresent(rc -> {
            // we use the loading status of DynamicConfiguration to decide whether ConfigCenter has been initiated.
            Environment.getInstance().getDynamicConfiguration().orElseGet(() -> {
                ConfigManager configManager = ConfigManager.getInstance();
                ConfigCenterConfig cc = configManager.getConfigCenter().orElse(new ConfigCenterConfig());
                cc.setProtocol(rc.getProtocol());
                cc.setAddress(rc.getAddress());
                cc.setHighestPriority(false);
                setConfigCenter(cc);
                startConfigCenter();
                return null;
            });
        });
    }

    /**
     * @return local
     * @deprecated Replace to <code>getStub()</code>
     */
    @Deprecated
    public String getLocal() {
        return local;
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(Boolean)</code>
     */
    @Deprecated
    public void setLocal(Boolean local) {
        if (local == null) {
            setLocal((String) null);
        } else {
            setLocal(local.toString());
        }
    }

    /**
     * @param local
     * @deprecated Replace to <code>setStub(String)</code>
     */
    @Deprecated
    public void setLocal(String local) {

    }

    public String getStub() {
        return stub;
    }

    public void setStub(Boolean stub) {
        if (stub == null) {
            setStub((String) null);
        } else {
            setStub(stub.toString());
        }
    }

    public void setStub(String stub) {
        checkName("stub", stub);
        this.stub = stub;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public Integer getConnections() {
        return connections;
    }

    public void setConnections(Integer connections) {
        this.connections = connections;
    }


    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getListener() {
        return listener;
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        ConfigManager.getInstance().setApplication(application);
        this.application = application;
    }

    private void createApplicationIfAbsent() {
        if (this.application != null) {
            return;
        }
        ConfigManager configManager = ConfigManager.getInstance();
        setApplication(
                configManager
                        .getApplication()
                        .orElseGet(() -> {
                            ApplicationConfig applicationConfig = new ApplicationConfig();
                            applicationConfig.refresh();
                            return applicationConfig;
                        })
        );
    }

    public ModuleConfig getModule() {
        return module;
    }

    public void setModule(ModuleConfig module) {
        ConfigManager.getInstance().setModule(module);
        this.module = module;
    }

    public RegistryConfig getRegistry() {
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
        registries.add(registry);
        setRegistries(registries);
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        ConfigManager.getInstance().addRegistries((List<RegistryConfig>) registries);
        this.registries = (List<RegistryConfig>) registries;
    }

    @Parameter(excluded = true)
    public String getRegistryIds() {
        return registryIds;
    }

    public void setRegistryIds(String registryIds) {
        this.registryIds = registryIds;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(String monitor) {
        setMonitor(new MonitorConfig(monitor));
    }

    public void setMonitor(MonitorConfig monitor) {
        ConfigManager.getInstance().setMonitor(monitor);
        this.monitor = monitor;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkMultiName("owner", owner);
        this.owner = owner;
    }

    public ConfigCenterConfig getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(ConfigCenterConfig configCenter) {
        ConfigManager.getInstance().setConfigCenter(configCenter);
        this.configCenter = configCenter;
    }

    public Integer getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(Integer callbacks) {
        this.callbacks = callbacks;
    }

    public String getOnconnect() {
        return onconnect;
    }

    public void setOnconnect(String onconnect) {
        this.onconnect = onconnect;
    }

    public String getOndisconnect() {
        return ondisconnect;
    }

    public void setOndisconnect(String ondisconnect) {
        this.ondisconnect = ondisconnect;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
