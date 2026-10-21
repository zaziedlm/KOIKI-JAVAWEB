package org.koikifw.reference.expense.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.reference.HomeController;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Uses isolated resources so normal tests never require an npm build. */
class ReferenceSpaWebConfigurationTest {
    @TempDir
    Path resources;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exposesOnlyThePackagedShellAndItsHomeLink(boolean packaged) throws Exception {
        if (packaged) {
            Path spa = resources.resolve("META-INF/resources/spa");
            Files.createDirectories(spa.resolve("assets"));
            Files.writeString(spa.resolve("index.html"), "<!doctype html><title>Fixture SPA</title>");
            Files.writeString(spa.resolve("assets/fixture.js"), "/* non-secret fixture */");
        }
        try (var loader = new URLClassLoader(new URL[] {resources.toUri().toURL()}, getClass().getClassLoader()) {
            @Override
            public @Nullable URL getResource(String name) {
                if (name.startsWith("META-INF/resources/spa/")) {
                    Path file = resources.resolve(name);
                    if (!Files.exists(file)) {
                        return null;
                    }
                    try {
                        return file.toUri().toURL();
                    } catch (java.net.MalformedURLException exception) {
                        throw new IllegalStateException(exception);
                    }
                }
                return super.getResource(name);
            }
        }) {
            new WebApplicationContextRunner().withClassLoader(loader)
                    .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, ThymeleafAutoConfiguration.class))
                    .withUserConfiguration(HomeController.class, ReferenceSpaWebConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.containsBean("referenceSpaWebConfiguration")).isEqualTo(packaged);
                        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
                        mvc.perform(get("/")).andExpect(status().isOk())
                                .andExpect(model().attribute("spaAvailable", packaged))
                                .andExpect(content().string(packaged
                                        ? containsString("Session SPAで経費申請を開く")
                                        : not(containsString("Session SPAで経費申請を開く"))));
                        for (String path : new String[] {"/spa", "/spa/"}) {
                            var result = mvc.perform(get(path));
                            if (packaged) {
                                result.andExpect(status().isOk()).andExpect(forwardedUrl("/spa/index.html"))
                                        .andExpect(header().string("Cache-Control", "no-store"));
                            } else {
                                result.andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
                            }
                        }
                        var index = mvc.perform(get("/spa/index.html"));
                        if (packaged) {
                            index.andExpect(status().isOk()).andExpect(content().string(containsString("Fixture SPA")))
                                    .andExpect(header().string("Cache-Control", "no-store"));
                        } else {
                            index.andExpect(status().isNotFound());
                        }
                        mvc.perform(get("/spa/unknown")).andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
                        mvc.perform(get("/spa/assets/missing.js")).andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
                    });
        }
    }
}
