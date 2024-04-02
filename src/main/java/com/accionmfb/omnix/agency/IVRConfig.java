package com.accionmfb.omnix.agency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author user on 31/10/2023
 */

@Configuration
@ComponentScan
@EntityScan("com.accionmfb.omnix.agency.model")
@PropertySource("classpath:application.properties")
@EnableTransactionManagement
public class IVRConfig {

    @Autowired
    Environment env;

//       IVR customerAccountTransactionManager


    @Bean(name = "customerAccountEntityManager")
    public LocalContainerEntityManagerFactoryBean customerAccountEntityManager() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(customerAccountDataSource());
        em.setPackagesToScan(new String[]{"com.accionmfb.omnix.agency.model"});
        em.setPersistenceUnitName("customerAccountPersistenceUnit");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        properties.setProperty("hibernate.dialect", env.getProperty("spring.jpa.hibernate.dialect"));
        em.setJpaProperties(properties);

        return em;
    }

    @Bean(name = "customerAccountTransactionManager")
    @DependsOn(value = "customerAccountEntityManager")
    public PlatformTransactionManager customerAccountTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(customerAccountEntityManager().getObject());
        return transactionManager;
    }

    @Bean(name = "customerAccountDataSource")
    public DataSource customerAccountDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.ds_customerAccount.db.driver-class-name"));
        dataSource.setUrl(env.getProperty("spring.ds_customerAccount.url"));
        dataSource.setUsername(env.getProperty("spring.ds_customerAccount.db.username"));
        dataSource.setPassword(env.getProperty("spring.ds_customerAccount.db.password"));
        return dataSource;
    }


    @Bean(name = "coreDataSource")
    public DataSource coreDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.ds_core.db.driver-class-name"));
        dataSource.setUrl(env.getProperty("spring.ds_core.url"));
        dataSource.setUsername(env.getProperty("spring.ds_core.db.username"));
        dataSource.setPassword(env.getProperty("spring.ds_core.db.password"));
        return dataSource;
    }

    @Bean(name = "coreEntityManager")
    public LocalContainerEntityManagerFactoryBean coreEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(coreDataSource());
        em.setPackagesToScan(new String[]{"com.accionmfb.omnix.agency.model"});
        em.setPersistenceUnitName("ivrCorePersistenceUnit");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto-ivr"));
        properties.setProperty("hibernate.dialect", env.getProperty("spring.jpa.hibernate.dialect"));
        em.setJpaProperties(properties);

        return em;
    }

    @Bean(name = "ivrCoreTransactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(coreEntityManagerFactory().getObject());

        return transactionManager;
    }

    @Bean
    public RestTemplate restTemplate () {
        return new RestTemplate();
    }
}
