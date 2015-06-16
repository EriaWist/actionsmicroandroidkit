package com.actionsmicro.androidkit.ezcast.imp.dlna.actions;

import java.util.logging.Logger;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;

public abstract class Next extends ActionCallback {

	private static Logger log = Logger.getLogger(Next.class.getName());


    public Next(Service service) {
        this(new UnsignedIntegerFourBytes(0), service);
    }

    public Next(UnsignedIntegerFourBytes instanceId, Service service) {
        super(new ActionInvocation(service.getAction("Next")));
        getActionInvocation().setInput("InstanceID", instanceId);
    }

    @Override
    public void success(ActionInvocation invocation) {
        log.fine("Execution successful");
    }

}
