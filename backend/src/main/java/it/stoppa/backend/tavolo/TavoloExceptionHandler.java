package it.stoppa.backend.tavolo;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = { "it.stoppa.backend.tavolo", "it.stoppa.backend.lobby" })
class TavoloExceptionHandler {

	@ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
	ResponseEntity<Map<String, String>> mossaNonValida(RuntimeException eccezione) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errore", eccezione.getMessage()));
	}

}
