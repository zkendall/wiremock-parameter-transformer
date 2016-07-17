package com.zkendall.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class WiremockTest {

    @BeforeClass
    public void setup() {
        int port = 8080;
        WireMockServer wireMockServer =
                new WireMockServer(new WireMockConfiguration()
                        .port(port)
                        .extensions(new TemplatingResponseBuilder()));
        wireMockServer.start();
        WireMock.configureFor("localhost", port);

        stubFor(post(urlEqualTo("/encode"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBodyFile("/test_Templated_Response.json")
                        .withStatus(200)));
    }

    @Test
    public void test() {
        String expectation = "{\n"
                + "  \"Name\": \"bob law's law blog\",\n"
                + "  \"customerId\": \"123\"\n"
                + "}\n";

        String request = "{ give-me: ${customerId:123}";

        RestTemplate template = new RestTemplate();
        final String s = template.postForObject("http://localhost:8080/encode", request, String.class);

        assertThat(s).isEqualTo(expectation);
    }

    @Test
    public void regex() {
        final TemplatingResponseBuilder builder = new TemplatingResponseBuilder();
        Map<String, String> map = builder.extractDefinitions("sample ${id:123}");
        assertThat(map.get("id")).isEqualTo("123");

        map = builder.extractDefinitions("sample ${id:123} body body ${kee:unique}");
        assertThat(map).contains(entry("id", "123"), entry("kee", "unique"));
    }


}
