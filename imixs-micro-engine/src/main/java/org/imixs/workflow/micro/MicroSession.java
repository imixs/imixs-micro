package org.imixs.workflow.micro;

/**
 * Defines the context of a processing life cycle
 */
public class MicroSession {

    String device;

    public MicroSession(String device) {
        this.device = device;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

}
