package org.koikifw.walkingskeleton.tier2.masterdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.Category;
import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.CategoryRepository;
import org.koikifw.walkingskeleton.tier2.masterdata.application.DeactivateCategoryUseCase.CategoryResult;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeactivateCategoryUseCaseTest {

    private static final UUID CATEGORY_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000001");

    @Mock
    private CategoryRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeactivateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeactivateCategoryUseCase(repository, eventPublisher);
    }

    @Test
    void publishesEventBeforeSavingInactiveCategory() {
        Category category = new Category(CATEGORY_ID, "travel", true);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(repository.save(category)).thenReturn(category);

        CategoryResult result = useCase.deactivate(CATEGORY_ID);

        InOrder order = inOrder(eventPublisher, repository);
        order.verify(eventPublisher).publishEvent(new CategoryDeactivating(CATEGORY_ID));
        order.verify(repository).save(category);
        assertThat(category.active()).isFalse();
        assertThat(result).isEqualTo(new CategoryResult(CATEGORY_ID, false));
    }

    @Test
    void leavesCategoryActiveWhenSynchronousReceiverRejectsEvent() {
        Category category = new Category(CATEGORY_ID, "travel", true);
        CategoryDeactivating event = new CategoryDeactivating(CATEGORY_ID);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        doThrow(new IllegalStateException("category is referenced by pending expenses"))
                .when(eventPublisher)
                .publishEvent(event);

        assertThatThrownBy(() -> useCase.deactivate(CATEGORY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("category is referenced by pending expenses");
        assertThat(category.active()).isTrue();
        verify(repository, never()).save(category);
    }

    @Test
    void rejectsAlreadyInactiveCategoryWithoutPublishingEvent() {
        Category category = new Category(CATEGORY_ID, "travel", false);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> useCase.deactivate(CATEGORY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("category is already inactive: " + CATEGORY_ID);
        verify(eventPublisher, never()).publishEvent(new CategoryDeactivating(CATEGORY_ID));
        verify(repository, never()).save(category);
    }
}
