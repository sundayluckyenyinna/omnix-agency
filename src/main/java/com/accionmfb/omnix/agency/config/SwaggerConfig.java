package com.accionmfb.omnix.agency.config;

import com.accionmfb.omnix.agency.model.AccionAgent;
import com.accionmfb.omnix.agency.model.Account;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

@Configuration
//public class SwaggerConfig implements CommandLineRunner {
public class SwaggerConfig {

    /**
     Method to configure swagger.
     @return documentation on the web
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.accionmfb.omnix.agency.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Agency banking Microservice API")
                .description("Complete REST Agency middleware microservice API consumable by web clients")
                .license("MIT License")
                .version("1.1.0")
                .licenseUrl("https://opensource.org/licenses/MIT")
                .build();
    }

//    @Autowired
//    AgencyRepository agencyRepository;

//    @Autowired
//    @Qualifier("omnixDatasource")
//    DataSource omnixDatasource;

//    @Override
//    public void run(String... args) {
//        List<AccionAgent> agent = agencyRepository.allAgents();
//        System.out.println("agentsssss ------------------------------------------------------->>>>>>>>>>>> {} " + agent);
//        try(Connection connection = omnixDatasource.getConnection()) {
//            DatabaseMetaData metaData = connection.getMetaData();
//            String dbName = metaData.getDatabaseProductName();
//            String dbVersion = metaData.getDatabaseProductVersion();
//            String dbUrl = metaData.getURL();
//
//            System.out.println("Connected to ---------->>>>> "+ dbName);
//            System.out.println("Database version ---------->>>>> "+ dbVersion);
//            System.out.println("Database URL ---------->>>>> "+ dbUrl);
//        }
//        catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
}
