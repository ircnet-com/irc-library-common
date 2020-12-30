package com.ircnet.library.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private boolean checkInheritance;

    private List<AbstractEventListener> eventListeners;

    public EventBus() {
        this.eventListeners = new ArrayList<>();
    }

    public void registerEventListener(int index, AbstractEventListener eventListener) {
        eventListeners.add(index, eventListener);
    }

    public void registerEventListener(AbstractEventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public void registerEventListener(String owner, AbstractEventListener eventListener) {
        eventListener.setOwner(owner);
        eventListeners.add(eventListener);
    }

    public void removeEventListener(AbstractEventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    public void removeEventListenersOfOwner(String owner) {
        Iterator<AbstractEventListener> iterator = eventListeners.iterator();

        while(iterator.hasNext()) {
            AbstractEventListener eventListener = iterator.next();

            if(owner.equals(eventListener.getOwner())) {
                iterator.remove();
            }
        }
    }

    public void setCheckInheritance(boolean checkInheritance) {
        this.checkInheritance = checkInheritance;
    }

    public void publishEvent(AbstractEvent event) {
        Iterator<AbstractEventListener> iterator = getEventListeners().iterator();
        
        while(iterator.hasNext()) {
            AbstractEventListener eventListener;

            try {
                eventListener = iterator.next();
            }
            catch(ConcurrentModificationException e) {
                return;
            }

            if(eventListener.getEventClass() == event.getClass() || (checkInheritance && eventListener.getEventClass().isInstance(event.getClass()))) {
                try {
                    eventListener.onEvent(event);
                }
                catch(ClassCastException e) {
                }
                catch(Exception e) {
                    LOGGER.error("An error occurred", e);
                }
            }
        }
    }

    public List<AbstractEventListener> getEventListeners() {
        return eventListeners;
    }

    public void setEventListeners(List<AbstractEventListener> eventListeners) {
        this.eventListeners = eventListeners;
    }
}
