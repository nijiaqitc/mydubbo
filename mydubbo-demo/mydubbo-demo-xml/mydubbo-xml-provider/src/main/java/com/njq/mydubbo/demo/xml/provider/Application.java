package com.njq.mydubbo.demo.xml.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author: nijiaqi
 * @date: 2019/6/10
 */
public class Application {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-provider.xml");
        context.start();
//        System.out.println(context.getBean("abc"));
//        System.out.println(context.getBean("bcb"));

        System.in.read();
    }
}
