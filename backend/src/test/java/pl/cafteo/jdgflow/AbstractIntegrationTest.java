package pl.cafteo.jdgflow;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {
    // Tests share the local jdgflow_test database (configured in application-test.yml).
    // Each test class is responsible for cleaning up its own data via repositories in @BeforeEach.
}
