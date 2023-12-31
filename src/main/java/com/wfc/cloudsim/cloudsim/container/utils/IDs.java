package com.wfc.cloudsim.cloudsim.container.utils;

/**
 * Created by sareh on 13/08/15.
 */

import com.wfc.cloudsim.cloudsim.container.containerPodProvisioners.ContainerPodPe;
import com.wfc.cloudsim.cloudsim.container.containerProvisioners.ContainerPe;
import com.wfc.cloudsim.cloudsim.container.core.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A factory for CloudSim entities' ids. CloudSim requires a lot of ids, that
 * are provided by the end user. This class is a utility for automatically
 * generating valid ids.
 * Modifies for containers
 *
 * @author nikolay.grozev
 */

public final class IDs {


    private static final Map<Class<?>, Integer> COUNTERS = new LinkedHashMap<>();
    private static final Set<Class<?>> NO_COUNTERS = new HashSet<>();
    private static int globalCounter = 1;

    static {
        COUNTERS.put(ContainerCloudlet.class, 1);
        COUNTERS.put(ContainerPod.class, 1);
        COUNTERS.put(Container.class, 1);
        COUNTERS.put(ContainerHost.class, 1);        
        COUNTERS.put(ContainerPe.class, 1);
        COUNTERS.put(ContainerPodPe.class, 1);
    }

    private IDs() {
    }

    public static synchronized void reset() {
        COUNTERS.put(ContainerCloudlet.class, 1);
        COUNTERS.put(ContainerPod.class, 1);
        COUNTERS.put(Container.class, 1);
        COUNTERS.put(ContainerHost.class, 1);
        COUNTERS.put(ContainerPe.class, 1);
        COUNTERS.put(ContainerPodPe.class, 1);
    }

    /**
     * Returns a valid id for the specified class.
     *
     * @param clazz - the class of the object to get an id for. Must not be null.
     * @return a valid id for the specified class.
     */
    public static synchronized int pollId(final Class<?> clazz) {
        Class<?> matchClass = null;
        if (COUNTERS.containsKey(clazz)) {
            matchClass = clazz;
        } else if (!NO_COUNTERS.contains(clazz)) {
            for (Class<?> key : COUNTERS.keySet()) {
                if (key.isAssignableFrom(clazz)) {
                    matchClass = key;
                    break;
                }
            }
        }

        int result = -1;
        if (matchClass == null) {
            NO_COUNTERS.add(clazz);
            result = pollGlobalId();
        } else {
            result = COUNTERS.get(matchClass);
            COUNTERS.put(matchClass, result + 1);
        }

        if (result < 0) {
            throw new IllegalStateException("The generated id for class:" + clazz.getName()
                    + " is negative. Possible integer overflow.");
        }

        return result;
    }

    private static synchronized int pollGlobalId() {
        return globalCounter++;
    }

}


