package com.ircnet.library.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private static EventBus instance;
    private boolean checkInheritance ;

    private List<AbstractEventListener> eventListeners;

    private EventBus() {
        this.eventListeners = new ArrayList<>();
    }

    public static EventBus getInstance() {
        if(instance == null) {
            instance = new EventBus();
            instance.checkInheritance = true;
        }

        return instance;
    }

    public static void registerEventListener(int index, AbstractEventListener eventListener) {
        getInstance().eventListeners.add(index, eventListener);
    }

    public static void registerEventListener(AbstractEventListener eventListener) {
        getInstance().eventListeners.add(eventListener);
    }

    public static void registerEventListener(String owner, AbstractEventListener eventListener) {
        eventListener.setOwner(owner);
        getInstance().eventListeners.add(eventListener);
    }

    public static void removeEventListener(AbstractEventListener eventListener) {
        getInstance().eventListeners.remove(eventListener);
    }

    public static void removeEventListenersOfOwner(String owner) {
        Iterator<AbstractEventListener> iterator = getInstance().eventListeners.iterator();

        while(iterator.hasNext()) {
            AbstractEventListener eventListener = iterator.next();

            if(owner.equals(eventListener.getOwner())) {
                iterator.remove();
            }
        }
    }

    public static void setCheckInheritance(boolean checkInheritance) {
        getInstance().checkInheritance = checkInheritance;
    }

    public static void publishEvent(AbstractEvent event) {
        Iterator<AbstractEventListener> iterator = getInstance().getEventListeners().iterator();
        
        while(iterator.hasNext()) {
            AbstractEventListener eventListener;

            try {
                eventListener = iterator.next();
            }
            catch(ConcurrentModificationException e) {
                return;
            }

            if(eventListener.getEventClass() == event.getClass() || (instance.checkInheritance && eventListener.getEventClass().getClass().isInstance(event.getClass()))) {
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
