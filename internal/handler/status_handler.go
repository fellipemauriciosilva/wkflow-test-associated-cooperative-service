// Package handler provides HTTP handler functions for the controlmesh-test service.
package handler

import (
	"encoding/json"
	"log"
	"net/http"
	"time"
)

// StatusResponse is the canonical JSON payload for GET /api/v1/status.
// Exactly three fields are defined by spec H26-04-VALIDACAO; no extras are
// permitted so that TestStatusHandler_FullPayloadShape can enforce the shape.
type StatusResponse struct {
	Service   string `json:"service"`
	Status    string `json:"status"`
	Timestamp string `json:"timestamp"`
}

// serviceName is the fixed identifier required by the spec.
// Defined as a package-level constant so tests can reference it without
// embedding a magic string.
const ServiceName = "controlmesh-test"

// StatusHandler handles GET /api/v1/status.
//
// Contract (spec H26-04-VALIDACAO):
//   - Returns HTTP 200
//   - Content-Type: application/json
//   - Body: {"service":"controlmesh-test","status":"ok","timestamp":"<RFC-3339>"}
//   - No database or external dependency
func StatusHandler(w http.ResponseWriter, r *http.Request) {
	// Enforce GET-only at the handler level so the bare handler is also safe
	// when mounted without a method-guard middleware.
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	payload := StatusResponse{
		Service:   ServiceName,
		Status:    "ok",
		Timestamp: time.Now().UTC().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	// Prevent proxies from caching health-check responses.
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(http.StatusOK)

	if err := json.NewEncoder(w).Encode(payload); err != nil {
		// Encoding to a ResponseRecorder / net.Conn should never fail, but log
		// defensively so the error is observable in structured logs.
		log.Printf("level=error handler=StatusHandler msg=\"failed to encode response\" error=%v", err)
	}
}
