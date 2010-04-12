package com.google.speedtracer.client.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * DataBag is a utility class to access {@link JavaScriptObject} fields
 * Any JSO can be casted to a DataBag, or you can use it statically.
 */
public class DataBag extends JavaScriptObject {
  protected DataBag() {
  }
  
  /* Member accessors */
  public final native boolean getBooleanProperty(String prop) /*-{
    return !!this[prop];
  }-*/;

  public final native double getDoubleProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native int getIntProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native JavaScriptObject getJSObjectProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native String getStringProperty(String prop) /*-{
    return this[prop];
  }-*/;
  
  public final native boolean hasOwnProperty(String prop) /*-{
    return this.hasOwnProperty(prop);
  }-*/;
  
  /*
   * Static Accessors
   */
  public final static native boolean getBooleanProperty(JavaScriptObject obj, String prop) /*-{
    return !!obj[prop];
  }-*/;
  
  public final static native double getDoubleProperty(JavaScriptObject obj, String prop) /*-{
    return obj[prop];
  }-*/;
  
  public final static native int getIntProperty(JavaScriptObject obj, String prop) /*-{
    return obj[prop];
  }-*/;
  
  public final static native JavaScriptObject getJSObjectProperty(JavaScriptObject obj, String prop) /*-{
    return obj[prop];
  }-*/;
  
  public final static native String getStringProperty(JavaScriptObject obj, String prop) /*-{
    return obj[prop];
  }-*/;
  
  public final static native boolean hasOwnProperty(JavaScriptObject obj, String prop) /*-{
    return obj.hasOwnProperty(prop);
  }-*/;
}