package com.gist.guild.node.spike.configuration;

import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {
    protected static final String ADMIN = "ADMIN";

    @Autowired
    ParticipantRepository participantRepository;

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        List<Participant> administrators = participantRepository.findByAdministratorTrue();
        Set<UserDetails> admins = new HashSet<>(administrators.size());
        for(Participant administrator : administrators) {
            UserDetails admin = User.withUsername(administrator.getTelegramUserId().toString())
                    .password(administrator.getAdminPasswordEncoded())
                    .roles(ADMIN)
                    .build();
            admins.add(admin);
        }
        return new InMemoryUserDetailsManager(admins);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
