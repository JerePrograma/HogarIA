package com.hogaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HogarIaApplication {
  public static void main(String[] args) { SpringApplication.run(HogarIaApplication.class, args); }
}
