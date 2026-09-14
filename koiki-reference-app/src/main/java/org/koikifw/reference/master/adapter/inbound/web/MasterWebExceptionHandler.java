package org.koikifw.reference.master.adapter.inbound.web;

import jakarta.servlet.http.HttpServletRequest;
import org.koikifw.reference.master.application.MasterFailure;
import org.koikifw.reference.master.application.MasterOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/** Produces sanitized full-page failures for master MVC requests. */
@ControllerAdvice(assignableTypes = MasterManagementController.class)
public class MasterWebExceptionHandler {

    @ExceptionHandler(MasterOperationException.class)
    ModelAndView handle(MasterOperationException exception, HttpServletRequest request) {
        MasterFailure failure = exception.failure();
        ModelAndView response = new ModelAndView(errorView(request));
        response.setStatus(switch (failure) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, CONCURRENT_MODIFICATION -> HttpStatus.CONFLICT;
            case DEPENDENCY_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
        });
        response.addObject("message", switch (failure) {
            case INVALID_INPUT -> "入力内容を確認してください。";
            case NOT_FOUND -> "対象のマスターは存在しません。";
            case CONFLICT -> "同じコードが存在するか、現在の状態では操作できません。";
            case CONCURRENT_MODIFICATION -> "他の操作で更新されています。再読み込みしてください。";
            case DEPENDENCY_FAILURE -> "マスター管理を一時的に利用できません。";
        });
        return response;
    }

    @ExceptionHandler(Exception.class)
    ModelAndView handleUnexpected(HttpServletRequest request) {
        ModelAndView response = new ModelAndView(errorView(request));
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.addObject("message", "予期しないエラーが発生しました。時間をおいて再度お試しください。");
        return response;
    }

    private static String errorView(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"))
                ? "master/error :: error"
                : "master/error";
    }
}
