/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ayache
 */
public class JMXBeanModelFactory {

    private static Map<String, BeanHolder> BEAN_MAP = new HashMap<>();

    private static class BeanHolder{
        Object bean;
        Object proxy;

        public BeanHolder(Object bean, Object proxy) {
            this.bean = bean;
            this.proxy = proxy;
        }
        
    }
    
    public static final <B, M extends B> M getModel(Class<M> model, final B bean, final String name) {
        BeanHolder result = BEAN_MAP.get(name);
        if (result == null || bean != result.bean) {
            Object newProxyInstance = Proxy.newProxyInstance(JMXBeanModelFactory.class.getClassLoader(), new Class[]{model}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        return method.invoke(bean, args);
                    } catch (Exception exception) {
//                        System.err.println(method);
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
