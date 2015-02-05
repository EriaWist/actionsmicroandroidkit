package com.actionsmicro.androidkit.ezcast.imp.dlna.actions;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;

import java.util.logging.Logger;

public abstract class SetNextAVTransportURI extends ActionCallback {

	private static Logger log = Logger.getLogger(SetNextAVTransportURI.class.getName());

    public SetNextAVTransportURI(Service service, String uri) {
        this(new UnsignedIntegerFourBytes(0), service, uri, null);
    }

    public SetNextAVTransportURI(Service service, String uri, String metadata) {
        this(new UnsignedIntegerFourBytes(0), service, uri, metadata);
    }

    public SetNextAVTransportURI(UnsignedIntegerFourBytes instanceId, Service service, String uri) {
        this(instanceId, service, uri, null);
    }

    public SetNextAVTransportURI(UnsignedIntegerFourBytes instanceId, Service service, String uri, String metadata) {
        super(new ActionInvocation(service.getAction("SetNextAVTransportURI")));
        log.fine("Creating SetNextAVTransportURI action for URI: " + uri);
        getActionInvocation().setInput("InstanceID", instanceId);
        getActionInvocation().setInput("NextURI", uri);
        getActionInvocation().setInput("NextURIMetaData", metadata);
    }

    @Override
    public void success(ActionInvocation invocation) {
        log.fine("Execution successful");
    }

}
