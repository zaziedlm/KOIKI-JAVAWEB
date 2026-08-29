package org.koikifw.cp6fixture;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItem;
import org.koikifw.runtimeconsumer.workreview.domain.model.WorkReview;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only fixture that deliberately violates the JPA Entity Web boundary. */
public final class Cp6EntityExposureFixture {

    private Cp6EntityExposureFixture() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {WorkItem.class, WorkReview.class, ExposedParent.class})
    public static class Configuration {

        @Bean
        ExposedParentUseCase exposedParentUseCase(EntityManager entityManager) {
            return new ExposedParentUseCase(entityManager);
        }

        @Bean
        ExposedParentController exposedParentController(ExposedParentUseCase useCase) {
            return new ExposedParentController(useCase);
        }
    }

    @Entity
    @Table(name = "cp6_exposed_parent")
    static class ExposedParent {

        @Id
        private UUID id = UUID.randomUUID();

        @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        @JoinColumn(name = "parent_id")
        private List<ExposedDetail> details = new ArrayList<>();

        protected ExposedParent() {
        }

        ExposedParent(UUID id, List<ExposedDetail> details) {
            this.id = id;
            this.details = new ArrayList<>(details);
        }

        public UUID getId() {
            return id;
        }

        public List<ExposedDetail> getDetails() {
            return details;
        }
    }

    @Entity
    @Table(name = "cp6_exposed_detail")
    static class ExposedDetail {

        @Id
        private UUID id = UUID.randomUUID();

        private String label = "";

        protected ExposedDetail() {
        }

        ExposedDetail(UUID id, String label) {
            this.id = id;
            this.label = label;
        }

        public UUID getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    static class ExposedParentUseCase {

        private final EntityManager entityManager;

        ExposedParentUseCase(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Transactional
        public ExposedParent loadDetachedEntity() {
            UUID id = UUID.randomUUID();
            ExposedParent parent = new ExposedParent(
                    id, List.of(new ExposedDetail(UUID.randomUUID(), "lazy-detail")));
            entityManager.persist(parent);
            entityManager.flush();
            entityManager.clear();
            return entityManager.find(ExposedParent.class, id);
        }
    }

    @RestController
    @RequestMapping("/api/{version}/test-only/entity-exposure")
    static class ExposedParentController {

        private final ExposedParentUseCase useCase;

        ExposedParentController(ExposedParentUseCase useCase) {
            this.useCase = useCase;
        }

        @GetMapping(version = "1")
        ExposedParent expose() {
            return useCase.loadDetachedEntity();
        }
    }
}
