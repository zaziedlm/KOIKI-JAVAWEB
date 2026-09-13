package org.koikifw.reference.expense.adapter.inbound.web;

import org.koikifw.reference.expense.application.ExpenseFailure;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/** Produces sanitized full-page failures for expense MVC requests. */
@ControllerAdvice(assignableTypes = ExpenseWebController.class)
public class ExpenseWebExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ModelAndView handleStatus(ResponseStatusException exception) {
        ModelAndView response = new ModelAndView("expense/error");
        response.setStatus(HttpStatus.valueOf(exception.getStatusCode().value()));
        response.addObject("message", exception.getStatusCode().value() == 404
                ? "申請が存在しないか、閲覧・操作できません。"
                : "入力内容を確認してください。");
        return response;
    }

    @ExceptionHandler(ExpenseOperationException.class)
    ModelAndView handle(ExpenseOperationException exception) {
        ExpenseFailure failure = exception.failure();
        ModelAndView response = new ModelAndView("expense/error");
        response.setStatus(switch (failure) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MASTER_UNAVAILABLE, INVALID_TRANSITION, SELF_DECISION, CONFLICT,
                    CONCURRENT_MODIFICATION -> HttpStatus.CONFLICT;
            case DEPENDENCY_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
        });
        response.addObject("message", switch (failure) {
            case INVALID_INPUT -> "入力内容を確認してください。";
            case MASTER_UNAVAILABLE -> "選択した部門または経費科目は現在利用できません。";
            case NOT_FOUND -> "申請が存在しないか、閲覧・操作できません。";
            case INVALID_TRANSITION -> "現在の申請状態ではこの操作を実行できません。";
            case SELF_DECISION -> "自分の申請を承認・却下・差戻しできません。";
            case CONCURRENT_MODIFICATION -> "他の操作で更新されています。再読み込みしてください。";
            case CONFLICT -> "現在の状態では操作を完了できません。";
            case DEPENDENCY_FAILURE -> "経費申請機能を一時的に利用できません。";
        });
        return response;
    }
}
