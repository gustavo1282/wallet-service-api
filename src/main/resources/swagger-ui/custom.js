(function () {
  // espera o Swagger UI subir
  const orig = window.SwaggerUIBundle;
  if (!orig) return;

  window.SwaggerUIBundle = function (config) {
    config.operationsSorter = function (a, b) {
      // a e b são Immutable Maps
      const opA = (a.get("operationId") || "").toLowerCase();
      const opB = (b.get("operationId") || "").toLowerCase();
      return opA.localeCompare(opB);
    };
    return orig(config);
  };
})();