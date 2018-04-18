package org.glassfish.jersey.server.spring;

import java.util.Collection;

/**
 * Since StringUtils of spring framework have been removed, this is a copy
 * of implementation from last stable 4.x version.
 * 
 * @author Askar Akhmerov
 */
public class StringUtils {

  public static String[] toStringArray(Collection<String> collection) {
    if (collection == null) {
      return null;
    }
    return collection.toArray(new String[collection.size()]);
  }

}
