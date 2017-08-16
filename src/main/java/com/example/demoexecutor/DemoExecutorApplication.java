package com.example.demoexecutor;

import com.hazelcast.cluster.memberselector.MemberSelectors;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.spring.context.SpringAware;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.function.Supplier;

@SpringBootApplication
@ImportResource("classpath:test.xml")
@Slf4j
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
	CommandLineRunner cli(@Qualifier("hz1") HazelcastInstance hz1, @Qualifier("hz2") HazelcastInstance hz2, @Value("${execute.remote}") boolean isremote) {
		return (args) -> {
			MemberSelector memberSelector = isremote ? MemberSelectors.NON_LOCAL_MEMBER_SELECTOR : MemberSelectors.LOCAL_MEMBER_SELECTOR;
			Member member = isremote ? hz2.getCluster().getLocalMember() : hz1.getCluster().getLocalMember();
			wrapTryCatch(() -> {
				hz1.getExecutorService("default").execute(new SomeRunnableTask(), memberSelector);
			});
			wrapTryCatch(() -> {
				hz1.getScheduledExecutorService("default").scheduleOnMember(new SomeRunnableTask(), member, 2000, TimeUnit.MILLISECONDS);
			});
		};
	}

	void wrapTryCatch(Runnable rn) {
		try {
			rn.run();
			Thread.sleep(3000);
		}catch (Exception e) {
			log.error("", e);
		}
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
