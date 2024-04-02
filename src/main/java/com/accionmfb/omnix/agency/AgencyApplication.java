package com.accionmfb.omnix.agency;

import com.accionmfb.omnix.agency.ivr.repository.BVNRepository;
import com.accionmfb.omnix.agency.ivr.repository.BvnJpaRepository;
import com.accionmfb.omnix.agency.ivr.repository.CustomerAccountRepository;
import com.accionmfb.omnix.agency.ivr.repository.IVRRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Import({IVRConfig.class, AppConfig.class})
@EnableHystrixDashboard
@EnableCircuitBreaker
@EnableEurekaClient
@EnableFeignClients
@ComponentScan(basePackages = "com.accionmfb.omnix.agency")
public class AgencyApplication {

    public static void main(String[] args) {

        SpringApplication.run(AgencyApplication.class, args);
    }


}
