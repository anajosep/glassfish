package org.glassfish.webservices;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeploymentContext;

import org.glassfish.api.admin.ServerEnvironment;

import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.container.EndpointRegistrationException;

import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.logging.LogDomains;
import com.sun.enterprise.deployment.*;


/**
 * This class implements the ApplicationContainer and will be used
 * to register endpoints to the grizzly ServletAdapter
 * Thus when a request is received it is directed to our EjbWebServiceServlet
 * so that it can process the request
 * 
 * @author Bhakti Mehta
 */

public class WebServicesApplication implements ApplicationContainer {

    private Collection<String> contextRoots;

    private com.sun.grizzly.tcp.Adapter adapter;

    private final RequestDispatcher dispatcher;

    private final ServerEnvironment serverEnvironment;

    private DeploymentContext deploymentCtx;

    protected Logger logger = LogDomains.getLogger(this.getClass(),LogDomains.WEBSERVICES_LOGGER);


    public WebServicesApplication(DeploymentContext context, ServerEnvironment env, RequestDispatcher dispatcherString ){
        this.deploymentCtx = context;
        this.dispatcher = dispatcherString;
        this.serverEnvironment = env;
        this.contextRoots = getContextRoots();
        this.adapter = (com.sun.grizzly.tcp.Adapter) new EjbWSAdapter();
    }
    public Object getDescriptor() {
        return null;
    }

    public boolean start(ApplicationContext startupContext) throws Exception {

        try {
            Application app = deploymentCtx.getModuleMetaData(Application.class);
            if ((app.getStandaloneBundleDescriptor() instanceof WebBundleDescriptor)
                    &&  ((!app.getStandaloneBundleDescriptor().getSpecVersion().equals("2.5")
                    || (!app.getStandaloneBundleDescriptor().hasWebServices()) ) )
                    ) {
                //JAXWS based apps
                //do nothing
            } else {
                Iterator<String> iter = contextRoots.iterator();
                String contextRoot = null;
                while(iter.hasNext()) {
                    contextRoot = iter.next();
                    dispatcher.registerEndpoint(contextRoot, (com.sun.grizzly.tcp.Adapter)adapter, this);
                }
            }
        } catch (EndpointRegistrationException e) {
            logger.log(Level.SEVERE,  "Error in registering the endpoint",e);

        }

        return true;
    }


    private Collection<String> getContextRoots() {
        contextRoots = new ArrayList<String>();
        Application app = deploymentCtx.getModuleMetaData(Application.class);
        if ((app.getStandaloneBundleDescriptor() instanceof WebBundleDescriptor)
                            &&  ((!app.getStandaloneBundleDescriptor().getSpecVersion().equals("2.5")
                                  || (!app.getStandaloneBundleDescriptor().hasWebServices()) ) )
                            ) {
            //JAXWS based apps
            //do nothing
        } else {
            Set<BundleDescriptor> bundles = app.getBundleDescriptors();
            for(BundleDescriptor bundle : bundles) {
                WebServicesDescriptor wsDesc = bundle.getWebServices();
                for (WebService ws : wsDesc.getWebServices()) {

                    for (WebServiceEndpoint endpoint:ws.getEndpoints() ){
                        contextRoots.add(endpoint.getEndpointAddressUri())  ;
                    }
                }
            }
        }

        return contextRoots;
    }

    public boolean stop(ApplicationContext stopContext) {
        try {
            Iterator<String> iter = contextRoots.iterator();
            String contextRoot = null;
            while(iter.hasNext()) {
                contextRoot = iter.next();
                dispatcher.unregisterEndpoint(contextRoot);
            }
        } catch (EndpointRegistrationException e) {
            logger.log(Level.SEVERE,  "Error in unregistering the endpoint",e);
            return false;
        }
        return true;
    }

    public boolean suspend() {
        return false;
    }

    public boolean resume() throws Exception {
        return false;
    }

    public ClassLoader getClassLoader() {
        return null;
    }
}
