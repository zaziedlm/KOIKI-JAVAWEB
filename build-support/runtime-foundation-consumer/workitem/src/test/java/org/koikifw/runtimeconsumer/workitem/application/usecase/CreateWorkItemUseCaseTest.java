package org.koikifw.runtimeconsumer.workitem.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItem;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.domain.event.WorkItemCreated;

class CreateWorkItemUseCaseTest {

    @Test
    void createsAValidPersistenceModel() {
        InMemoryRepository repository = new InMemoryRepository();
        CapturingPublisher publisher = new CapturingPublisher();
        CreateWorkItemUseCase useCase = new CreateWorkItemUseCase(repository, publisher);

        UUID id = useCase.create("example");

        assertEquals(id, repository.saved.getId());
        assertEquals("example", repository.saved.getLabel());
        assertEquals(new WorkItemCreated(id, "example"), publisher.published);
    }

    @Test
    void rejectsBlankLabelInTheApplicationLayer() {
        CreateWorkItemUseCase useCase =
                new CreateWorkItemUseCase(new InMemoryRepository(), event -> { });

        assertThrows(IllegalArgumentException.class, () -> useCase.create(" "));
    }

    private static final class InMemoryRepository implements WorkItemRepository {

        private WorkItem saved = new WorkItem(UUID.randomUUID(), "initial");

        @Override
        public Optional<WorkItem> findById(UUID id) {
            return saved.getId().equals(id) ? Optional.of(saved) : Optional.empty();
        }

        @Override
        public WorkItem save(WorkItem workItem) {
            saved = workItem;
            return workItem;
        }
    }

    private static final class CapturingPublisher
            implements org.springframework.context.ApplicationEventPublisher {

        private Object published = new Object();

        @Override
        public void publishEvent(Object event) {
            published = event;
        }
    }
}
