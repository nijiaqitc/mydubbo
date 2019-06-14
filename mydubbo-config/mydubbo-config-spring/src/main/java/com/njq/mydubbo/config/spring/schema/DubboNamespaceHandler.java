package com.njq.mydubbo.config.spring.schema;

import com.njq.mydubbo.config.spring.ServiceBean;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author: nijiaqi
 * @date: 2019/6/10
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        System.out.println("类初始化完成------------");
    }

}
