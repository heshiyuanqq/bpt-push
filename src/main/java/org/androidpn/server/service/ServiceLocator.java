package org.androidpn.server.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

public class ServiceLocator implements BeanFactoryAware {
    private static BeanFactory beanFactory = null;

    private static ServiceLocator servlocator = null;

    public static String TERMINAL_SERVICE = "terminalService";

    public static String NOTIFICATION_SERVICE = "notificationService";

    @SuppressWarnings("static-access")
	public void setBeanFactory(BeanFactory factory) throws BeansException {
	this.beanFactory = factory;
    }

    public BeanFactory getBeanFactory() {
    	return beanFactory;
    }

    public static ServiceLocator getInstance() {
    	System.err.println("***************beanFactory="+beanFactory+"******************************");
		if (servlocator == null){
			 servlocator = (ServiceLocator) beanFactory.getBean("serviceLocator");
		}
		return servlocator;
    }


    public static Object getService(String servName) {
	return beanFactory.getBean(servName);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object getService(String servName, Class clazz) {
	return beanFactory.getBean(servName, clazz);
    }

    public static NotificationService getNotificationService() {
	return (NotificationService) getService(NOTIFICATION_SERVICE);
    }
}
