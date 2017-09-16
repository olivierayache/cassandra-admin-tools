/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerInvocationHandler;

/**
 *
 * @author Ayache
 */
public class JMXBeanModelFactory {

    private static final Map<String, BeanHolder> BEAN_MAP = new HashMap<>();
    private static final Map<String, Method> METHODS = new HashMap<>();

    private static class BeanHolder{
        Object bean;
        Object proxy;

        public BeanHolder(Object bean, Object proxy) {
            this.bean = bean;
            this.proxy = proxy;
        }
        
    }
    
    public static final void editBean(final Object bean, final String name, final String value) {
        try {
            Method editMethod = METHODS.get(name);
            if (METHODS.get(name) == null) {
                Class<? extends Object> aClass = bean.getClass();
                for (Method m : aClass.getMethods()) {
                    if (m.getName().toLowerCase().substring(3).equals(name.toLowerCase()) && m.getName().startsWith("set") && m.getParameterCount() == 1) {
                        METHODS.put(name, m);
                        editMethod = m;
                    }
                }
            }
            if (editMethod == null) {
                throw new NoSuchMethodError(name);
            }
            Object val;
            if (editMethod.getParameterTypes()[0].isPrimitive()){
                if (boolean.class == editMethod.getParameterTypes()[0]){
                    val = Boolean.valueOf(value);
                }else if (int.class == editMethod.getParameterTypes()[0]){
                    val = Integer.valueOf(value);
                }else if (short.class == editMethod.getParameterTypes()[0]){
                    val = Short.valueOf(value);
                }else if (long.class == editMethod.getParameterTypes()[0]){
                    val = Long.valueOf(value);
                }else if (float.class == editMethod.getParameterTypes()[0]){
                    val = Float.valueOf(value);
                }else if (double.class == editMethod.getParameterTypes()[0]){
                    val = Double.valueOf(value);
                }else{
                    throw new UnsupportedOperationException("Only boolean and number types are supported");
                }
            }else if (String.class == editMethod.getParameterTypes()[0]){
                val = value;
            } else {
                throw new UnsupportedOperationException("Only String is supported");
            }
            editMethod.invoke(bean, val);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(JMXBeanModelFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
    public static final <B, M extends B> M getModel(Class<M> model, final B bean, final String name) {
        BeanHolder result = BEAN_MAP.get(name);
        if (result == null || bean != result.bean) {
            List<String> editables = new ArrayList<>();
            try {
                MBeanServerInvocationHandler handler = (javax.management.MBeanServerInvocationHandler) Proxy.getInvocationHandler(bean);
                MBeanAttributeInfo[] attributes = handler.getMBeanServerConnection().getMBeanInfo(handler.getObjectName()).getAttributes();
                for (MBeanAttributeInfo attribute : attributes) {
                    if (attribute.isWritable()) {
                        editables.add(attribute.getName());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(JMXBeanModelFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
            Object newProxyInstance = Proxy.newProxyInstance(model.getClassLoader(), new Class[]{model}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("getEditables")) {
                        return editables;
                    }
                    try {
                        return method.invoke(bean, args);
                    } catch (Exception exception) {
                        if (method.getReturnType().isPrimitive()) {
                            if (method.getReturnType().isAssignableFrom(boolean.class)) {
                                return false;
                            }
                            if (method.getReturnType().isAssignableFrom(int.class)) {
                                return -1;
                            }
                        }
                        return null;
                    }
                }
            });
             
            BEAN_MAP.put(name, new BeanHolder(bean, newProxyInstance));
            return (M) newProxyInstance;
        }
        return (M) result.proxy;
        }
    }
