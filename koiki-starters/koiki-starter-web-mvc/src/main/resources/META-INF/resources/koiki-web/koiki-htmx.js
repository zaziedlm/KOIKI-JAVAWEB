(function () {
  "use strict";

  function metaContent(name) {
    var element = document.querySelector('meta[name="' + name + '"]');
    return element ? element.getAttribute("content") : null;
  }

  function requestTarget(event) {
    return event.detail.target || event.detail.elt || null;
  }

  document.addEventListener("htmx:configRequest", function (event) {
    var token = metaContent("_csrf");
    var header = metaContent("_csrf_header");
    if (token && header) {
      event.detail.headers[header] = token;
    }
  });

  document.addEventListener("htmx:beforeRequest", function (event) {
    var target = requestTarget(event);
    if (target) {
      target.setAttribute("aria-busy", "true");
    }
  });

  document.addEventListener("htmx:afterRequest", function (event) {
    var target = requestTarget(event);
    if (target) {
      target.removeAttribute("aria-busy");
    }
  });

  document.addEventListener("htmx:beforeSwap", function (event) {
    var responseUrl = event.detail.xhr.responseURL || "";
    if (responseUrl.endsWith("/login")) {
      event.detail.shouldSwap = false;
      window.location.assign(responseUrl);
      return;
    }
    var status = event.detail.xhr.status;
    var contentType = event.detail.xhr.getResponseHeader("Content-Type") || "";
    if (status >= 400 && status < 600 && contentType.indexOf("text/html") === 0) {
      event.detail.shouldSwap = true;
      event.detail.isError = false;
    }
  });

  document.addEventListener("htmx:afterSwap", function (event) {
    var target = event.detail.target;
    var focusTarget = target.matches("[data-koiki-focus]")
      ? target
      : target.querySelector("[data-koiki-focus]");
    if (focusTarget) {
      focusTarget.focus();
    }
    document.dispatchEvent(new CustomEvent("koiki:htmx:afterSwap", {
      detail: { target: target }
    }));
  });
}());
