package com.googlecode.vicovre.recordings.event;

import ag3.interfaces.types.EventDescription;

public interface EventListener {

    void processEvent(EventDescription event);

    void connectionClosed();
}
