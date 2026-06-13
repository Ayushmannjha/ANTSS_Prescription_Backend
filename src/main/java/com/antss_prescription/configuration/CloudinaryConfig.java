package com.antss_prescription.configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.key")
    private String key;
    @Value("${cloudinary.secret")
    private String secret;
    @Value("${cloudinary.cloud-name")
    private String cloud_name;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", cloud_name,
                        "api_key", key,
                        "api_secret", secret
                )
        );
    }
}
