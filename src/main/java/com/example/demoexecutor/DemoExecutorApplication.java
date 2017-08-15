package com.example.demoexecutor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@ImportResource("classpath:hazelcast.xml")
public class DemoExecutorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoExecutorApplication.class, args);
	}

	@Bean
	@Scope("singleton")
	MyBean myBean() {
		return new MyBean();
	}

	@Bean
	CommandLineRunner cli(HazelcastInstance hz) {
		return (args) -> {
			hz.getExecutorService("default").execute(new SomeRunnableTask());
			Thread.sleep(1000);
			hz.getScheduledExecutorService("default").schedule(new SomeRunnableTask(), 2000, TimeUnit.MILLISECONDS);
		};
	}

	public class MyBean {
		public void print() {
			System.out.println("here");
		}
	}

	@SpringAware
	public static class SomeRunnableTask implements Runnable, Serializable, ApplicationContextAware {

		private static final long serialVersionUID = 1L;

		private transient ApplicationContext context;

		@Autowired
		private transient MyBean myBean;

		@Override
		public void run() {
			context.getBean(MyBean.class).print();
			myBean.print();
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			context = applicationContext;
		}
	}
}
