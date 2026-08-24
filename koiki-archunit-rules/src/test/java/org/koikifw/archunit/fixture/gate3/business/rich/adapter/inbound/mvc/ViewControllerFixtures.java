package org.koikifw.archunit.fixture.gate3.business.rich.adapter.inbound.mvc;

import org.koikifw.archunit.fixture.gate3.business.rich.application.RichUseCase;
import org.koikifw.archunit.fixture.gate3.business.rich.domain.model.RichModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

public final class ViewControllerFixtures {

    private final RichUseCase useCase = new RichUseCase();
    private final RichModel stored = useCase.load();

    @GetMapping("/direct-model")
    public String directModel(Model model) {
        model.addAttribute("rich", useCase.load());
        return "rich";
    }

    @GetMapping("/direct-constructor")
    public String directConstructor(Model model) {
        model.addAttribute("rich", new RichModel(3L));
        return "rich";
    }

    @GetMapping("/direct-model-and-view")
    public ModelAndView directModelAndView() {
        return new ModelAndView("rich", "rich", useCase.load());
    }

    @GetMapping("/direct-add-object")
    public ModelAndView directAddObject() {
        ModelAndView modelAndView = new ModelAndView("rich");
        modelAndView.addObject("rich", useCase.load());
        return modelAndView;
    }

    @GetMapping("/converted")
    public String converted(Model model) {
        RichModel domainModel = useCase.load();
        RichView view = RichView.from(domainModel);
        model.addAttribute("rich", view);
        return "rich";
    }

    @GetMapping("/helper")
    public String helper(Model model) {
        RichModel domainModel = loadThroughHelper();
        model.addAttribute("rich", RichView.from(domainModel));
        return "rich";
    }

    @GetMapping("/field")
    public String field(Model model) {
        model.addAttribute("rich", stored);
        return "rich";
    }

    @GetMapping("/reflection")
    public String reflection(Model model) throws ReflectiveOperationException {
        Object reflected = RichModel.class.getConstructor(Long.class).newInstance(4L);
        model.addAttribute("rich", reflected);
        return "rich";
    }

    private RichModel loadThroughHelper() {
        return useCase.load();
    }

    public record RichView(Long id) {

        static RichView from(RichModel model) {
            return new RichView(model.id());
        }
    }
}
