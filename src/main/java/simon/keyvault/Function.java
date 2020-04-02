package simon.keyvault;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.integration.CacheLoader;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {

    final static String keyVaultUrl=System.getenv()
        .getOrDefault("KEYVAULT_URL","https://simon-testar-keyvault.vault.azure.net/");
    final static String secretName = System.getenv()
        .getOrDefault("SECRET_NAME","secret_name");

   final static SecretClient secretClient = new SecretClientBuilder()
        .vaultUrl(keyVaultUrl)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();

    final  static Cache<String, KeyVaultSecret> secretsCache = createCache();

    static private Cache<String, KeyVaultSecret> createCache() {
        return Cache2kBuilder.of(String.class, KeyVaultSecret.class)
            .name("secrets")
            .expireAfterWrite(5, TimeUnit.MINUTES)    // expire/refresh after 5 minutes
            .resilienceDuration(30, TimeUnit.SECONDS) // cope with at most 30 seconds outage before propagating exceptions
            .refreshAhead(true)                       // keep fresh when expiring
            .loader(new CacheLoader<String, KeyVaultSecret>() {
                @Override
                public KeyVaultSecret load(final String key) throws Exception {
                    System.out.println("fetch secret "+key);
                    return secretClient.getSecret(key);
                }
            })
            .build();
    }

    static String env="";
    static {
       System.getenv().entrySet().stream().forEach(e -> env+=(e.getKey()+" = "+e.getValue())+"\n" );
    }
    static String classpath="";
    static {

       final ClassLoader cl = ClassLoader.getSystemClassLoader();
        for (URL url : ((URLClassLoader) cl).getURLs()) {

            final String u = url.toString();
             String[] split;
            if(!u.contains("azure-functions-java-worker")){
                split = u.split("/");
                classpath+=split[split.length-1]+" ";
            } else
                classpath+=u+" ";
        }
    }

    public Function(){
    }
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        context.getLogger().info("classpath "+classpath);

     final KeyVaultSecret secret = secretsCache.get(secretName);

        return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + secret.getValue()).build();

      // return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + "mej").build();


    }

}
