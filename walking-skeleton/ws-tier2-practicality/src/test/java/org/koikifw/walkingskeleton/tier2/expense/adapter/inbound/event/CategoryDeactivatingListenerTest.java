package org.koikifw.walkingskeleton.tier2.expense.adapter.inbound.event;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.application.VerifyCategoryDeactivationUseCase;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryDeactivatingListenerTest {

    @Mock
    private VerifyCategoryDeactivationUseCase useCase;

    @InjectMocks
    private CategoryDeactivatingListener listener;

    @Test
    void delegatesPublishedCategoryIdExactlyOnce() {
        UUID categoryId = UUID.fromString("30000000-0000-0000-0000-000000000001");

        listener.onCategoryDeactivating(new CategoryDeactivating(categoryId));

        verify(useCase).verify(categoryId);
    }
}
