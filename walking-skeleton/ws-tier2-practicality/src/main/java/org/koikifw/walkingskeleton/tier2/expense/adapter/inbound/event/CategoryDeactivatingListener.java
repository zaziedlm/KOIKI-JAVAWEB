package org.koikifw.walkingskeleton.tier2.expense.adapter.inbound.event;

import org.koikifw.walkingskeleton.tier2.expense.application.VerifyCategoryDeactivationUseCase;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CategoryDeactivatingListener {

    private final VerifyCategoryDeactivationUseCase useCase;

    public CategoryDeactivatingListener(VerifyCategoryDeactivationUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void onCategoryDeactivating(CategoryDeactivating event) {
        useCase.verify(event.categoryId());
    }
}
