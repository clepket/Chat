package pt.isec.PD_API;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import pt.isec.Security.AuthorizationFilter;
import pt.isec.Interfaces.RemoteServer;
import java.rmi.Naming;
import java.rmi.Remote;
import javax.servlet.Filter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@ComponentScan(basePackages = {"pt.isec.Controllers"})
@SpringBootApplication
public class PdApiApplication {

	public static RemoteServer remoteServer;
	public static RemoteServer broadcastServer;

	public static void main(String[] args) {

		try{
			String registration = "rmi_server";
			Remote remoteService = Naming.lookup(registration);
			remoteServer = (RemoteServer) remoteService;

		}catch(Exception e){
			System.out.println("Erro ao aceder a serviço rmi: " + e);
		}

		SpringApplication.run(PdApiApplication.class, args);
	}

	@EnableWebSecurity
	@Configuration
	class WebSecurityConfig extends WebSecurityConfigurerAdapter
	{
		@Override
		protected void configure(HttpSecurity http) throws Exception
		{
			http.csrf().disable()
					.addFilterAfter((Filter) new AuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
					.authorizeRequests()
					.antMatchers(HttpMethod.POST, "/user/login").permitAll()
					.anyRequest().authenticated().and()
					.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
					.exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

		}
	}

}