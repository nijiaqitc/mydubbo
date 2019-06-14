package com.njq.mydubbo.demo.xml.provider;

import com.njq.mydubbo.demo.inface.DemoService;

/**
 * @author: nijiaqi
 * @date: 2019/6/10
 */
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
//        return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();?
        return "11";
    }
}
